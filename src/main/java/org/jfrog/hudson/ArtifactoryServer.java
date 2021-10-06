/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson;

import com.google.common.collect.Lists;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.util.XStream2;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.usageReport.UsageReporter;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.jfrog.hudson.util.converters.ArtifactoryServerConverter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jfrog.hudson.JFrogPlatformInstance.DEFAULT_CONNECTION_TIMEOUT;
import static org.jfrog.hudson.JFrogPlatformInstance.DEFAULT_DEPLOYMENT_THREADS_NUMBER;
import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

/**
 * Represents an instance of jenkins artifactory configuration page.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryServer implements Serializable {
    private static final Logger log = Logger.getLogger(ArtifactoryServer.class.getName());

    private String url;
    private String id;
    // Network timeout in seconds to use both for connection establishment and for unanswered requests
    private int timeout = DEFAULT_CONNECTION_TIMEOUT;
    private boolean bypassProxy;
    // This object is set to Integer instead of int so upon starting if it's missing (due to upgrade from previous version)
    // This object will be null instead of 0. In the ArtifactoryBuilder there is a check if the object is null then we are
    // setting to 3 that is the default.
    private Integer connectionRetry;
    private Integer deploymentThreads;
    /**
     * List of repository keys, last time we checked. Copy on write semantics.
     */
    private transient volatile List<String> repositories;

    private transient volatile List<VirtualRepository> virtualRepositories;

    /**
     * @deprecated: Use org.jfrog.hudson.ArtifactoryServer#getDeployerCredentials()()
     */
    @Deprecated
    private Credentials deployerCredentials;
    /**
     * @deprecated: Use org.jfrog.hudson.ArtifactoryServer#getResolverCredentials()
     */
    @Deprecated
    private Credentials resolverCredentials;

    private CredentialsConfig deployerCredentialsConfig;

    private CredentialsConfig resolverCredentialsConfig;

    public ArtifactoryServer(String serverId, String artifactoryUrl, CredentialsConfig deployerCredentialsConfig,
                             CredentialsConfig resolverCredentialsConfig, int timeout, boolean bypassProxy, Integer connectionRetry, Integer deploymentThreads) {
        this.url = StringUtils.removeEnd(artifactoryUrl, "/");
        this.id = serverId;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.resolverCredentialsConfig = resolverCredentialsConfig;
        this.timeout = timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT;
        this.bypassProxy = bypassProxy;
        this.connectionRetry = connectionRetry != null ? connectionRetry : 3;
        this.deploymentThreads = deploymentThreads != null && deploymentThreads > 0 ? deploymentThreads : DEFAULT_DEPLOYMENT_THREADS_NUMBER;
    }

    public String getServerId() {
        return id;
    }

    public void setServerId(String id) {
        this.id = id;
    }

    public void setArtifactoryUrl(String url) {
        this.url = url;
    }

    public String getArtifactoryUrl() {
        return url != null ? url : getServerId();
    }

    public CredentialsConfig getDeployerCredentialsConfig() {
        return deployerCredentialsConfig;
    }

    public CredentialsConfig getResolverCredentialsConfig() {
        return resolverCredentialsConfig;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isBypassProxy() {
        return bypassProxy;
    }


    public int getConnectionRetry() {
        if (connectionRetry == null) {
            connectionRetry = 3;
        }
        return connectionRetry;
    }

    public void setConnectionRetry(int connectionRetry) {
        this.connectionRetry = connectionRetry;
    }

    public int getDeploymentThreads() {
        return deploymentThreads == null ? DEFAULT_DEPLOYMENT_THREADS_NUMBER : deploymentThreads;
    }

    public List<String> getLocalRepositoryKeys(Credentials credentials) throws IOException {
        try (ArtifactoryManager artifactoryManager = createArtifactoryManager(credentials, createProxyConfiguration())) {
            repositories = artifactoryManager.getLocalRepositoriesKeys();
        } catch (IOException e) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.WARNING, "Could not obtain local repositories list from '" + url + "'", e);
            } else {
                log.log(Level.WARNING,
                        "Could not obtain local repositories list from '" + url + "': " + e.getMessage());
            }
            throw e;
        }
        return repositories;
    }

    public List<String> getReleaseRepositoryKeysFirst(DeployerOverrider deployerOverrider, Item item) throws IOException {
        CredentialsConfig credentialsConfig = CredentialManager.getPreferredDeployer(deployerOverrider, this);
        List<String> repositoryKeys = getLocalRepositoryKeys(credentialsConfig.provideCredentials(item));
        if (repositoryKeys == null || repositoryKeys.isEmpty()) {
            return Lists.newArrayList();
        }
        Collections.sort(repositoryKeys, new RepositoryComparator());
        return repositoryKeys;
    }

    public List<String> getSnapshotRepositoryKeysFirst(DeployerOverrider deployerOverrider, Item item) throws IOException {
        CredentialsConfig credentialsConfig = CredentialManager.getPreferredDeployer(deployerOverrider, this);
        List<String> repositoryKeys = getLocalRepositoryKeys(credentialsConfig.provideCredentials(item));
        if (repositoryKeys == null || repositoryKeys.isEmpty()) {
            return Lists.newArrayList();
        }
        Collections.sort(repositoryKeys, Collections.reverseOrder(new RepositoryComparator()));
        return repositoryKeys;
    }

    public Map getStagingStrategy(PluginSettings selectedStagingPlugin, String buildName, Item item) throws IOException {
        CredentialsConfig resolvingCredentialsConfig = getResolvingCredentialsConfig();
        try (ArtifactoryManager artifactoryManager = createArtifactoryManager(resolvingCredentialsConfig.provideCredentials(item),
                createProxyConfiguration())) {
            return artifactoryManager.getStagingStrategy(selectedStagingPlugin.getPluginName(), buildName,
                    selectedStagingPlugin.getParamMap());
        }
    }

    public List<VirtualRepository> getVirtualRepositoryKeys(ResolverOverrider resolverOverrider, Item item) {
        CredentialsConfig preferredResolver = CredentialManager.getPreferredResolver(resolverOverrider, this);
        try (ArtifactoryManager artifactoryManager = createArtifactoryManager(preferredResolver.provideCredentials(item),
                createProxyConfiguration())) {
            virtualRepositories = RepositoriesUtils.generateVirtualRepos(artifactoryManager);
        } catch (IOException e) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.WARNING, "Could not obtain virtual repositories list from '" + url + "'", e);
            } else {
                log.log(Level.WARNING,
                        "Could not obtain virtual repositories list from '" + url + "': " + e.getMessage());
            }
            return Lists.newArrayList();
        }

        return virtualRepositories;
    }

    public boolean isArtifactoryPro(DeployerOverrider deployerOverrider, Item item) {
        CredentialsConfig credentialsConfig = CredentialManager.getPreferredDeployer(deployerOverrider, this);
        Credentials credentials = credentialsConfig.provideCredentials(item);
        try {
            ArtifactoryManager artifactoryManager = new ArtifactoryManager(url, credentials.getUsername(), credentials.getPassword(), new NullLog());
            return !artifactoryManager.getVersion().isOSS();
        } catch (IOException e) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.WARNING, "Could not obtain artifactory version from '" + url + "'", e);
            } else {
                log.log(Level.WARNING,
                        "Could not obtain artifactory version from '" + url + "': " + e.getMessage());
            }
        }
        return false;
    }

    public List<UserPluginInfo> getStagingUserPluginInfo(DeployerOverrider deployerOverrider, Item item) {
        List<UserPluginInfo> infosToReturn = Lists.newArrayList(UserPluginInfo.NO_PLUGIN);
        gatherUserPluginInfo(infosToReturn, "staging", deployerOverrider, item);
        return infosToReturn;
    }

    public List<UserPluginInfo> getPromotionsUserPluginInfo(DeployerOverrider deployerOverrider, Item item) {
        List<UserPluginInfo> infosToReturn = Lists.newArrayList(UserPluginInfo.NO_PLUGIN);
        gatherUserPluginInfo(infosToReturn, "promotions", deployerOverrider, item);
        return infosToReturn;
    }

    /**
     * This method might run on slaves, this is why we provide it with a proxy from the master config
     */
    public ArtifactoryManager createArtifactoryManager(Credentials credentials, ProxyConfiguration proxyConfiguration) {
        return createArtifactoryManager(credentials, proxyConfiguration, new NullLog());
    }

    public ArtifactoryManager createArtifactoryManager(Credentials credentials, ProxyConfiguration proxyConfiguration, Log logger) {
        ArtifactoryManagerBuilder artifactoryManagerBuilder = createArtifactoryManagerBuilder(credentials, proxyConfiguration, logger);
        return artifactoryManagerBuilder.build();
    }

    public ArtifactoryManagerBuilder createArtifactoryManagerBuilder(Credentials credentials, ProxyConfiguration proxyConfiguration, Log logger) {
        ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder();
        artifactoryManagerBuilder.setServerUrl(url).setUsername(credentials.getUsername()).setPassword(credentials.getPassword())
                .setAccessToken(credentials.getAccessToken()).setLog(logger).setConnectionRetry(getConnectionRetry())
                .setConnectionTimeout(timeout);
        if (!bypassProxy) {
            artifactoryManagerBuilder.setProxyConfiguration(proxyConfiguration);
        }
        return artifactoryManagerBuilder;
    }


    /**
     * Set the retry params for ArtifactoryManager
     *
     * @param artifactoryManager - the ArtifactoryManager to set the params.
     */
    private void setRetryParams(ArtifactoryManager artifactoryManager) {
        RepositoriesUtils.setRetryParams(getConnectionRetry(), artifactoryManager);
    }

    /**
     * This method might run on slaves, this is why we provide it with a proxy from the master config
     */
    public ArtifactoryManager createArtifactoryManager(Credentials credentials, ProxyConfiguration proxyConfiguration, TaskListener listener) {
        ArtifactoryManager artifactoryManager = new ArtifactoryManager(url, credentials.getUsername(),
                credentials.getPassword(), credentials.getAccessToken(), new JenkinsBuildInfoLog(listener));
        artifactoryManager.setConnectionTimeout(timeout);
        setRetryParams(artifactoryManager);
        if (!bypassProxy && proxyConfiguration != null) {
            artifactoryManager.setProxyConfiguration(proxyConfiguration.host, proxyConfiguration.port, proxyConfiguration.username,
                    proxyConfiguration.password);
        }

        return artifactoryManager;
    }

    /**
     * Decides what are the preferred credentials to use for resolving the repo keys of the server
     *
     * @return Preferred credentials for repo resolving. Never null.
     */
    public CredentialsConfig getResolvingCredentialsConfig() {
        if (resolverCredentialsConfig != null && resolverCredentialsConfig.isCredentialsProvided()) {
            return getResolverCredentialsConfig();
        }
        if (deployerCredentialsConfig != null) {
            return getDeployerCredentialsConfig();
        }

        return CredentialsConfig.EMPTY_CREDENTIALS_CONFIG;
    }

    private void gatherUserPluginInfo(List<UserPluginInfo> infosToReturn, String pluginKey, DeployerOverrider deployerOverrider, Item item) {
        CredentialsConfig credentialsConfig = CredentialManager.getPreferredDeployer(deployerOverrider, this);
        try (ArtifactoryManager artifactoryManager = createArtifactoryManager(credentialsConfig.provideCredentials(item),
                createProxyConfiguration())) {
            Map<String, List<Map>> userPluginInfo = artifactoryManager.getUserPluginInfo();
            if (userPluginInfo != null && userPluginInfo.containsKey(pluginKey)) {
                List<Map> stagingUserPluginInfo = userPluginInfo.get(pluginKey);
                if (stagingUserPluginInfo != null) {
                    for (Map stagingPluginInfo : stagingUserPluginInfo) {
                        infosToReturn.add(new UserPluginInfo(stagingPluginInfo));
                    }
                    Collections.sort(infosToReturn, new Comparator<UserPluginInfo>() {
                        public int compare(UserPluginInfo o1, UserPluginInfo o2) {
                            return o1.getPluginName().compareTo(o2.getPluginName());
                        }
                    });
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to obtain user plugin info: " + e.getMessage());
        }
    }

    private static class RepositoryComparator implements Comparator<String>, Serializable {
        public int compare(String o1, String o2) {
            if (o1.contains("snapshot") && !o2.contains("snapshot")) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    /**
     * Log setter for jobs that are using ArtifactoryManager which
     * creates the manager with NullLog object.
     *
     * @param listener           the listener of the job
     * @param artifactoryManager the ArtifactoryManager that was created
     */
    public void setLog(TaskListener listener, ArtifactoryManager artifactoryManager) {
        artifactoryManager.setLog(new JenkinsBuildInfoLog(listener));
    }

    /**
     * Page Converter
     */
    public static final class ConverterImpl extends ArtifactoryServerConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }
    }

    public void reportUsage(String stepName, WorkflowRun build, Log logger) {
        try {
            CredentialsConfig config = this.getDeployerCredentialsConfig();
            if (config == null) {
                config = this.getResolverCredentialsConfig();
            }
            Credentials credentials = config.provideCredentials(build.getParent());
            String[] featureIdArray = new String[]{stepName};
            UsageReporter usageReporter = new UsageReporter("jenkins-artifactory-plugin/" + ActionableHelper.getArtifactoryPluginVersion(), featureIdArray);
            usageReporter.reportUsage(this.getArtifactoryUrl(), credentials.getUsername(), credentials.getPassword(), "", Utils.getProxyConfiguration(this), logger);
            logger.debug("Usage info sent successfully.");
        } catch (Exception ex) {
            logger.error("Failed sending usage report to Artifactory: " + ex);
        }
    }
}
