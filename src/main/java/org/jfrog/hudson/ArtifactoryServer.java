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
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.jfrog.hudson.util.converters.ArtifactoryServerConverter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

/**
 * Represents an artifactory instance.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryServer implements Serializable {
    private static final Logger log = Logger.getLogger(ArtifactoryServer.class.getName());

    private static final int DEFAULT_CONNECTION_TIMEOUT = 300;    // 5 Minutes
    private static final int DEFAULT_DEPLOYMENT_THREADS_NUMBER = 3;
    private final String url;
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

    @DataBoundConstructor
    public ArtifactoryServer(String serverId, String artifactoryUrl, CredentialsConfig deployerCredentialsConfig,
                             CredentialsConfig resolverCredentialsConfig, int timeout, boolean bypassProxy, Integer connectionRetry, Integer deploymentThreads) {
        this.url = StringUtils.removeEnd(artifactoryUrl, "/");
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.resolverCredentialsConfig = resolverCredentialsConfig;
        this.timeout = timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT;
        this.bypassProxy = bypassProxy;
        this.id = serverId;
        this.connectionRetry = connectionRetry != null ? connectionRetry : 3;
        this.deploymentThreads = deploymentThreads != null && deploymentThreads > 0 ? deploymentThreads : DEFAULT_DEPLOYMENT_THREADS_NUMBER;
    }

    public String getServerId() {
        return id;
    }

    public void setServerId(String id) {
        this.id = id;
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

    // To populate the dropdown list from the jelly
    public List<Integer> getConnectionRetries() {
        List<Integer> items = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            items.add(i);
        }
        return items;
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

    /**
     * Return number of deployment threads.
     * To populate the dropdown list from the jelly:
     * <j:forEach var="r" items="${server.deploymentsThreads}">
     */
    @SuppressWarnings("unused")
    public List<Integer> getDeploymentsThreads() {
        List<Integer> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            items.add(i);
        }
        return items;
    }

    /**
     * Set the number of deployment threads.
     * Jelly uses reflection here and 'getDeploymentsThreads()' to get the data by the method and variable (matching) names
     * <f:option selected="${r==server.deploymentThreads}"
     *
     * @param deploymentThreads - Deployment threads number
     */
    @SuppressWarnings("unused")
    public void setDeploymentThreads(int deploymentThreads) {
        this.deploymentThreads = deploymentThreads;
    }

    public int getDeploymentThreads() {
        return deploymentThreads == null ? DEFAULT_DEPLOYMENT_THREADS_NUMBER : deploymentThreads;
    }

    public List<String> getLocalRepositoryKeys(Credentials credentials) throws IOException {
        try (ArtifactoryBuildInfoClient client = createArtifactoryClient(credentials, createProxyConfiguration())) {
            repositories = client.getLocalRepositoriesKeys();
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
        try (ArtifactoryBuildInfoClient client = createArtifactoryClient(resolvingCredentialsConfig.provideCredentials(item),
                createProxyConfiguration())) {
            return client.getStagingStrategy(selectedStagingPlugin.getPluginName(), buildName,
                    selectedStagingPlugin.getParamMap());
        }
    }

    public List<VirtualRepository> getVirtualRepositoryKeys(ResolverOverrider resolverOverrider, Item item) {
        CredentialsConfig preferredResolver = CredentialManager.getPreferredResolver(resolverOverrider, this);
        try (ArtifactoryBuildInfoClient client = createArtifactoryClient(preferredResolver.provideCredentials(item),
                createProxyConfiguration())) {
            virtualRepositories = RepositoriesUtils.generateVirtualRepos(client);
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
            ArtifactoryHttpClient client = new ArtifactoryHttpClient(url, credentials.getUsername(), credentials.getPassword(), new NullLog());
            ArtifactoryVersion version = client.getVersion();
            return version.hasAddons();
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
    public ArtifactoryBuildInfoClient createArtifactoryClient(Credentials credentials, ProxyConfiguration proxyConfiguration) {
        return createArtifactoryClient(credentials, proxyConfiguration, new NullLog());
    }

    public ArtifactoryBuildInfoClient createArtifactoryClient(Credentials credentials, ProxyConfiguration proxyConfiguration, Log logger) {
        ArtifactoryBuildInfoClientBuilder clientBuilder = createBuildInfoClientBuilder(credentials, proxyConfiguration, logger);
        return clientBuilder.build();
    }

    public ArtifactoryBuildInfoClientBuilder createBuildInfoClientBuilder(Credentials credentials, ProxyConfiguration proxyConfiguration, Log logger) {
        ArtifactoryBuildInfoClientBuilder clientBuilder = new ArtifactoryBuildInfoClientBuilder();
        clientBuilder.setArtifactoryUrl(url).setUsername(credentials.getUsername()).setPassword(credentials.getPassword())
                .setAccessToken(credentials.getAccessToken()).setLog(logger).setConnectionRetry(getConnectionRetry())
                .setConnectionTimeout(timeout);
        if (!bypassProxy) {
            clientBuilder.setProxyConfiguration(proxyConfiguration);
        }

        return clientBuilder;
    }

    public ArtifactoryDependenciesClientBuilder createDependenciesClientBuilder(Credentials credentials, ProxyConfiguration proxyConfiguration, Log logger) {
        ArtifactoryDependenciesClientBuilder clientBuilder = new ArtifactoryDependenciesClientBuilder();
        clientBuilder.setArtifactoryUrl(url).setUsername(credentials.getUsername())
                .setPassword(credentials.getPassword()).setLog(logger).setConnectionRetry(getConnectionRetry())
                .setConnectionTimeout(timeout);
        if (!bypassProxy) {
            clientBuilder.setProxyConfiguration(proxyConfiguration);
        }

        return clientBuilder;
    }

    /**
     * Set the retry params for the base client
     *
     * @param client - the client to set the params.
     */
    private void setRetryParams(ArtifactoryBaseClient client) {
        RepositoriesUtils.setRetryParams(getConnectionRetry(), client);
    }

    /**
     * This method might run on slaves, this is why we provide it with a proxy from the master config
     */
    public ArtifactoryDependenciesClient createArtifactoryDependenciesClient(Credentials credentials, ProxyConfiguration proxyConfiguration, TaskListener listener) {

        ArtifactoryDependenciesClient client = new ArtifactoryDependenciesClient(url, credentials.getUsername(),
                credentials.getPassword(), credentials.getAccessToken(), new JenkinsBuildInfoLog(listener));
        client.setConnectionTimeout(timeout);
        setRetryParams(client);
        if (!bypassProxy && proxyConfiguration != null) {
            client.setProxyConfiguration(proxyConfiguration.host, proxyConfiguration.port, proxyConfiguration.username,
                    proxyConfiguration.password);
        }

        return client;
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
        try (ArtifactoryBuildInfoClient client = createArtifactoryClient(credentialsConfig.provideCredentials(item),
                createProxyConfiguration())) {
            Map<String, List<Map>> userPluginInfo = client.getUserPluginInfo();
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
     * Log setter for jobs that are using createArtifactoryClient which
     * creates the client with NullLog object.
     *
     * @param listener the listener of the job
     * @param client   the client that was created
     */
    public void setLog(TaskListener listener, ArtifactoryBaseClient client) {
        client.setLog(new JenkinsBuildInfoLog(listener));
    }

    /**
     * Page Converter
     */
    public static final class ConverterImpl extends ArtifactoryServerConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }
    }
}
