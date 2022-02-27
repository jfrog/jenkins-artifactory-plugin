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

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import hudson.util.XStream2;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.client.Version;
import org.jfrog.build.extractor.clientConfiguration.client.ManagerBase;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.DistributionManager;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesHttpClient;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.jfrog.hudson.util.converters.ArtifactoryBuilderConverter;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

/**
 * @author Yossi Shaul
 */
public class ArtifactoryBuilder extends GlobalConfiguration {

    /**
     * Descriptor for {@link ArtifactoryBuilder}. Used as a singleton. The class is marked as public so that it can be
     * accessed from views.
     * <p/>
     * <p/>
     * See <tt>views/hudson/plugins/artifactory/ArtifactoryBuilder/*.jelly</tt> for the actual HTML fragment for the
     * configuration screen.
     */
    @Extension
    // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {

        private boolean useCredentialsPlugin;
        private List<JFrogPlatformInstance> jfrogInstances;
        /**
         * @deprecated: Use org.jfrog.hudson.ArtifactoryBuilder.DescriptorImpl#getJFrogInstances()
         */
        @Deprecated
        private List<ArtifactoryServer> artifactoryServers;
        private JFrogPipelinesServer jfrogPipelinesServer = new JFrogPipelinesServer();

        public DescriptorImpl() {
            super(ArtifactoryBuilder.class);
            load();
        }

        @SuppressWarnings("unused")
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins != null && jenkins.hasPermission(Jenkins.ADMINISTER)) {
                return PluginsUtils.fillPluginCredentials(project, ACL.SYSTEM);
            }
            return new StandardListBoxModel();
        }

        /**
         * Performs on-the-fly validation of the form field 'InstanceId'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        @SuppressWarnings("unused")
        public FormValidation doCheckInstanceId(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error("Please set server ID");
            }
            List<JFrogPlatformInstance> JFrogPlatformInstances = RepositoriesUtils.getJFrogPlatformInstances();
            if (JFrogPlatformInstances == null) {
                return FormValidation.ok();
            }
            int countServersByValueAsName = 0;
            for (JFrogPlatformInstance JFrogPlatformInstance : JFrogPlatformInstances) {
                if (JFrogPlatformInstance.getId().equals(value)) {
                    countServersByValueAsName++;
                    if (countServersByValueAsName > 1) {
                        return FormValidation.error("Duplicated JFrog platform instances ID");
                    }
                }
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        @RequirePOST
        public FormValidation doTestConnection(
                @QueryParameter("platformUrl") final String url,
                @QueryParameter("artifactoryUrl") final String artifactoryUrl,
                @QueryParameter("distributionUrl") final String distributionUrl,
                @QueryParameter("instance.timeout") final String timeout,
                @QueryParameter("instance.bypassProxy") final boolean bypassProxy,
                @QueryParameter("useCredentialsPlugin") final boolean useCredentialsPlugin,
                @QueryParameter("credentialsId") final String deployerCredentialsId,
                @QueryParameter("username") final String deployerCredentialsUsername,
                @QueryParameter("password") final String deployerCredentialsPassword,
                @QueryParameter("connectionRetry") final int connectionRetry) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.error("Testing the connection requires 'Administer' permission");
            }
            if (StringUtils.isBlank(artifactoryUrl) && StringUtils.isBlank(url)) {
                return FormValidation.error("Please set a valid Artifactory or platform URL");
            }
            if (connectionRetry < 0) {
                return FormValidation.error("Connection Retries can not be less then 0");
            }
            if (StringUtils.isEmpty(deployerCredentialsId) && (StringUtils.isEmpty(deployerCredentialsUsername) || StringUtils.isEmpty(deployerCredentialsPassword))) {
                return FormValidation.error("Please set a valid credentials");
            }
            String targetArtUrl = StringUtils.isBlank(artifactoryUrl) ? StringUtils.removeEnd(url, "/") + "/artifactory" : artifactoryUrl;
            String targetDisUrl = StringUtils.isBlank(distributionUrl) && StringUtils.isNotBlank(url) ? StringUtils.removeEnd(url, "/") + "/distribution" : distributionUrl;
            String accessToken = StringUtils.EMPTY;
            String username = StringUtils.EMPTY;
            String password = StringUtils.EMPTY;

            StringCredentials accessTokenCredentials = PluginsUtils.accessTokenCredentialsLookup(deployerCredentialsId, null);
            if (accessTokenCredentials != null) {
                accessToken = accessTokenCredentials.getSecret().getPlainText();
            } else {
                if (useCredentialsPlugin) {
                    Credentials credentials = PluginsUtils.usernamePasswordCredentialsLookup(deployerCredentialsId, null);
                    username = credentials.getUsername();
                    password = credentials.getPassword();
                } else {
                    username = deployerCredentialsUsername;
                    password = Secret.fromString(deployerCredentialsPassword).getPlainText();
                }
            }

            ArtifactoryManager artifactoryManager;
            DistributionManager distributionManager;
            if (StringUtils.isNotEmpty(username) || StringUtils.isNotEmpty(accessToken)) {
                artifactoryManager = new ArtifactoryManager(targetArtUrl, username, password, accessToken, new NullLog());
                distributionManager = new DistributionManager(targetDisUrl, username, password, accessToken, new NullLog());
            } else {
                artifactoryManager = new ArtifactoryManager(targetArtUrl, new NullLog());
                distributionManager = new DistributionManager(targetDisUrl, new NullLog());
            }
            try {
                Stream.of(artifactoryManager, distributionManager).forEach(manager -> {
                    if (!bypassProxy && Jenkins.get().proxy != null) {
                        manager.setProxyConfiguration(createProxyConfiguration());
                    }
                    if (StringUtils.isNotBlank(timeout)) {
                        manager.setConnectionTimeout(Integer.parseInt(timeout));
                    }
                });
                String outputMessage = createTestConnectionMessage(artifactoryManager, targetArtUrl, "JFrog Artifactory");
                if (StringUtils.isNotEmpty(targetDisUrl)) {
                    outputMessage += String.format("\n%s", createTestConnectionMessage(distributionManager, targetDisUrl, "JFrog Distribution"));
                }
                return FormValidation.ok(outputMessage);
            } finally {
                artifactoryManager.close();
                distributionManager.close();
            }
        }

        private String createTestConnectionMessage(ManagerBase manager, String targetUrl, String serverName) {
            try {
                Version version = manager.getVersion();
                if (version.isNotFound()) {
                    return String.format("%s not found at %s", serverName, targetUrl);
                } else {
                    return String.format("Found %s %s at %s", serverName, manager.getVersion().toString(), targetUrl);
                }
            } catch (ClientProtocolException e) {
                // If http/https are missing, ClientProtocolException will be thrown.
                // Since this kind of exception hold the error message inside 'cause' we must catch it to show a proper error message on Jenkins UI.
                return String.format("An error occurred while connecting to %s:%s%s", serverName, System.lineSeparator(), e.getCause().getMessage());
            } catch (Exception e) {
                return String.format("An error occurred while connecting to %s:%s%s", serverName, System.lineSeparator(), e.getMessage());
            }
        }

        @SuppressWarnings("unused")
        @RequirePOST
        public FormValidation doTestJFrogPipelinesConnection(
                @QueryParameter("pipelinesIntegrationUrl") final String url,
                @QueryParameter("pipelinesTimeout") final String timeout,
                @QueryParameter("pipelinesBypassProxy") final boolean bypassProxy,
                @QueryParameter("credentialsId") final String credentialsId,
                @QueryParameter("pipelinesConnectionRetries") final int connectionRetry) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.error("Testing the connection requires 'Administer' permission");
            }
            if (StringUtils.isBlank(url)) {
                return FormValidation.error("Please set a valid JFrog Pipelines integration URL");
            }
            if (connectionRetry < 0) {
                return FormValidation.error("Connection Retries can not be less then 0");
            }

            StringCredentials accessTokenCredentials = PluginsUtils.accessTokenCredentialsLookup(credentialsId, null);
            if (accessTokenCredentials == null) {
                return FormValidation.error("Please set credentials with access token as 'Secret text'");
            }
            String accessToken = accessTokenCredentials.getSecret().getPlainText();

            try (JFrogPipelinesHttpClient client = new JFrogPipelinesHttpClient(url, accessToken, new NullLog())) {
                if (!bypassProxy) {
                    ProxyConfiguration proxyConfiguration = createProxyConfiguration();
                    if (proxyConfiguration != null) {
                        client.setProxyConfiguration(proxyConfiguration);
                    }
                }

                if (StringUtils.isNotBlank(timeout)) {
                    client.setConnectionTimeout(Integer.parseInt(timeout));
                }
                client.setConnectionRetries(connectionRetry);

                Version version;
                try {
                    version = client.verifyCompatibleVersion();
                } catch (UnsupportedOperationException uoe) {
                    return FormValidation.warning(uoe.getMessage());
                } catch (Exception e) {
                    return FormValidation.error(e.getMessage());
                }
                return FormValidation.ok("Found JFrog Pipelines " + version.toString());
            }
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Artifactory Plugin";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins != null && jenkins.hasPermission(Jenkins.ADMINISTER)) {
                boolean useCredentialsPlugin = BooleanUtils.isTrue((Boolean) o.get("useCredentialsPlugin"));
                configureJFrogInstances(req, o);
                configureJFrogPipelinesServer(o);
                if (useCredentialsPlugin && !this.useCredentialsPlugin) {
                    resetJobsCredentials();
                    resetServersCredentials();
                }
                this.useCredentialsPlugin = useCredentialsPlugin;
                save();
                return super.configure(req, o);
            }
            throw new FormException("User doesn't have permissions to save", "Server ID");
        }

        private void configureJFrogInstances(StaplerRequest req, JSONObject o) throws FormException {
            List<JFrogPlatformInstance> jfrogInstances = new ArrayList<>();
            Object jfrogInstancesObj = o.get("jfrogInstances"); // an array or single object
            if (!JSONNull.getInstance().equals(jfrogInstancesObj)) {
                jfrogInstances = req.bindJSONToList(JFrogPlatformInstance.class, jfrogInstancesObj);
            }

            if (!isJFrogInstancesIDConfigured(jfrogInstances)) {
                throw new FormException("Please set the Instance ID.", "InstanceID");
            }

            if (isInstanceDuplicated(jfrogInstances)) {
                throw new FormException("The JFrog instance ID you have entered is already configured", "Instance ID");
            }

            if (isEmptyUrls(jfrogInstances)) {
                throw new FormException("Please set the The JFrog Platform or Artifactory URL", "URL");
            }
            autoFillPlatformServers(jfrogInstances);
            setJfrogInstances(jfrogInstances);
        }

        private void configureJFrogPipelinesServer(JSONObject o) {
            JSONObject credentialsConfigJson = (JSONObject) o.get("credentialsConfig");
            if (credentialsConfigJson == null) {
                return;
            }
            String credentialsId = credentialsConfigJson.optString("credentialsId");
            CredentialsConfig credentialsConfig = new CredentialsConfig("", "", credentialsId);
            credentialsConfig.setIgnoreCredentialPluginDisabled(true);
            JFrogPipelinesServer jfrogPipelinesServer = new JFrogPipelinesServer(o.getString("pipelinesIntegrationUrl"),
                    credentialsConfig, o.optInt("pipelinesTimeout"), o.getBoolean("pipelinesBypassProxy"),
                    o.optInt("pipelinesConnectionRetries"));
            setJfrogPipelinesServer(jfrogPipelinesServer);
        }

        private void resetServersCredentials() {
            for (JFrogPlatformInstance server : jfrogInstances) {
                if (server.getResolverCredentialsConfig() != null) {
                    server.getResolverCredentialsConfig().deleteCredentials();
                }
                if (server.getDeployerCredentialsConfig() != null) {
                    server.getDeployerCredentialsConfig().deleteCredentials();
                }
            }
        }

        private void resetJobsCredentials() {
            List<BuildableItemWithBuildWrappers> jobs = Jenkins.get().getAllItems(BuildableItemWithBuildWrappers.class);
            for (BuildableItem job : jobs) {
                ResolverOverrider resolver = ActionableHelper.getResolverOverrider(job);
                if (resolver != null) {
                    if (resolver.getResolverCredentialsConfig() != null) {
                        resolver.getResolverCredentialsConfig().deleteCredentials();
                    }
                }
                DeployerOverrider deployer = ActionableHelper.getDeployerOverrider(job);
                if (deployer != null) {
                    if (deployer.getDeployerCredentialsConfig() != null) {
                        deployer.getDeployerCredentialsConfig().deleteCredentials();
                    }
                }
                if (resolver != null || deployer != null) {
                    try {
                        job.save();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public JFrogPipelinesServer getJfrogPipelinesServer() {
            return jfrogPipelinesServer;
        }

        public boolean getUseCredentialsPlugin() {
            return useCredentialsPlugin;
        }

        public void setJfrogPipelinesServer(JFrogPipelinesServer jfrogPipelinesServer) {
            this.jfrogPipelinesServer = jfrogPipelinesServer;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public void setUseCredentialsPlugin(boolean useCredentialsPlugin) {
            this.useCredentialsPlugin = useCredentialsPlugin;
        }

        private boolean isEmptyUrls(List<JFrogPlatformInstance> jfrogInstances) {
            if (jfrogInstances == null) {
                return false;
            }
            for (JFrogPlatformInstance instance : jfrogInstances) {
                if (StringUtils.isBlank(instance.getUrl()) && StringUtils.isBlank(instance.getArtifactory().getArtifactoryUrl())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Autofill logic takes care whenever new JFrog instances are going to be saved, on the Jenkins configuration page.
         * Its main purpose to make sure that Artifactory & Distribution URLs were updated if JFrog platform URL changing.
         * The following actions take place:
         * 1. If JFrog platform URL exists but Artifactory is missing -> autofill the missing fields with <platform-url>/artifactory (same for distribution)
         * 2. If JFrog platform URL updated but Artifactory hasn't, override it with the new Platform URL e.g. <new-platform-url>/artifactory (same for distribution)
         *
         * @param newJFrogInstances New JFrog instances to be saved
         */
        public void autoFillPlatformServers(List<JFrogPlatformInstance> newJFrogInstances) {
            if (newJFrogInstances == null) {
                return;
            }
            for (JFrogPlatformInstance newInstance : newJFrogInstances) {
                if (StringUtils.isBlank(newInstance.getUrl())) {
                    continue;
                }
                if (StringUtils.isBlank(newInstance.getArtifactoryUrl())) {
                    setDefaultArtifactoryUrl(newInstance);
                }
                if (StringUtils.isBlank(newInstance.getDistributionUrl())) {
                    setDefaultDistributionUrl(newInstance);
                }
                Optional<JFrogPlatformInstance> preSavedInstance = getPreSavedInstance(newInstance.getId());
                if (!preSavedInstance.isPresent()) {
                    continue;
                }
                if (!isPlatformUrlChangedSinceLastSave(preSavedInstance.get(), newInstance)) {
                    continue;
                }
                // Check if Artifactory URL has a different prefix than platform URL.
                if (!StringUtils.startsWithIgnoreCase(newInstance.getArtifactoryUrl(), newInstance.getUrl())) {
                    // Check if the new Artifactory URL has changed since last time by comparing the URLs.
                    if (!isArtifactoryUrlChangedSinceLastSave(preSavedInstance.get(), newInstance)) {
                        // At this point, the Platform URL is different than the Artifactory URL.
                        // Artifactory URL has a different prefix than Platform URL.
                        // Artifactory URL has not changed, compared to last time configuration.
                        // Since Artifactory URL is hidden in Jenkins UI under the advanced tab, the user may forget to change the Artifactory URL along with the platform.
                        // As a result, override Artifactory URL.
                        setDefaultArtifactoryUrl(newInstance);
                    }
                }
                // Check if Distribution URL has a different prefix than platform URL.
                if (!StringUtils.startsWithIgnoreCase(newInstance.getDistributionUrl(), newInstance.getUrl())) {
                    // Check if the new Distribution URL has changed since last time by comparing the URLs.
                    if (!isDistributionUrlChangedSinceLastSave(preSavedInstance.get(), newInstance)) {
                        setDefaultDistributionUrl(newInstance);
                    }
                }
            }
        }

        private void setDefaultArtifactoryUrl(JFrogPlatformInstance newInstance) {
            newInstance.getArtifactory().setArtifactoryUrl(newInstance.getUrl() + "/artifactory");
        }

        private void setDefaultDistributionUrl(JFrogPlatformInstance newInstance) {
            newInstance.setDistributionUrl(newInstance.getUrl() + "/distribution");

        }

        private Optional<JFrogPlatformInstance> getPreSavedInstance(String id) {
            if (jfrogInstances == null) {
                return Optional.empty();
            }
            return jfrogInstances.stream().filter(inst -> inst.getId().equals(id)).findFirst();
        }

        private boolean isArtifactoryUrlChangedSinceLastSave(JFrogPlatformInstance oldInstance, JFrogPlatformInstance newInstance) {
            return !oldInstance.getArtifactory().getArtifactoryUrl().equals(newInstance.getArtifactory().getArtifactoryUrl());
        }

        private boolean isPlatformUrlChangedSinceLastSave(JFrogPlatformInstance oldInstance, JFrogPlatformInstance newInstance) {
            String oldUrl = oldInstance.getUrl();
            if (StringUtils.isEmpty(oldUrl)) {
                return StringUtils.isNotEmpty(newInstance.getUrl());
            }
            return !oldInstance.getUrl().equals(newInstance.getUrl());
        }

        private boolean isDistributionUrlChangedSinceLastSave(JFrogPlatformInstance oldInstance, JFrogPlatformInstance newInstance) {
            return !oldInstance.getDistributionUrl().equals(newInstance.getDistributionUrl());
        }

        private boolean isInstanceDuplicated(List<JFrogPlatformInstance> jfrogInstances) {
            Set<String> serversNames = new HashSet<>();
            if (jfrogInstances == null) {
                return false;
            }
            for (JFrogPlatformInstance instance : jfrogInstances) {
                String id = instance.getId();
                if (serversNames.contains(id)) {
                    return true;
                }
                serversNames.add(id);
            }
            return false;
        }

        private boolean isJFrogInstancesIDConfigured(List<JFrogPlatformInstance> jfrogInstances) {
            if (jfrogInstances == null) {
                return true;
            }
            for (JFrogPlatformInstance server : jfrogInstances) {
                String platformId = server.getId();
                String artifactoryId = server.getArtifactory().getServerId();
                if (StringUtils.isBlank(platformId) || StringUtils.isBlank(artifactoryId)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Used by Jenkins Jelly for displaying values.
         */
        public List<JFrogPlatformInstance> getJfrogInstances() {
            return jfrogInstances;
        }

        /**
         * Used by Jenkins Jelly for setting values.
         */
        public void setJfrogInstances(List<JFrogPlatformInstance> jfrogInstances) {
            this.jfrogInstances = jfrogInstances;
        }

        /**
         * Page Converter
         */
        public static final class ConverterImpl extends ArtifactoryBuilderConverter {
            public ConverterImpl(XStream2 xstream) {
                super(xstream);
            }
        }
    }
}
