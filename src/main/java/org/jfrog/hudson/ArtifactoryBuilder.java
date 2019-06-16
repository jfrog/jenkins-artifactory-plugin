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
import hudson.model.*;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        private List<ArtifactoryServer> artifactoryServers;

        public DescriptorImpl() {
            super(ArtifactoryBuilder.class);
            load();
        }

        @SuppressWarnings("unused")
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null && jenkins.hasPermission(Jenkins.ADMINISTER)) {
                return PluginsUtils.fillPluginCredentials(project, ACL.SYSTEM);
            }
            return new StandardListBoxModel();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a name");
            }
            if (value.length() < 4) {
                return FormValidation.warning("Isn't the name too short?");
            }
            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form field 'ServerId'.
         *
         * @param value          This parameter receives the value that the user has typed.
         * @param artifactoryUrl This parameter receives the value that the user has typed as artifactory Url.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckServerId(@QueryParameter String value, @QueryParameter String artifactoryUrl, @QueryParameter String username, @QueryParameter String password, @QueryParameter String credentialsId) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set server ID");
            }
            List<ArtifactoryServer> artifactoryServers = RepositoriesUtils.getArtifactoryServers();
            if (artifactoryServers == null) {
                return FormValidation.ok();
            }
            int countServersByValueAsName = 0;
            for (ArtifactoryServer server : artifactoryServers) {
                if (server.getName().equals(value)) {
                    countServersByValueAsName++;
                    if (countServersByValueAsName > 1) {
                        return FormValidation.error("Duplicated server ID");
                    }
                }
            }
            return FormValidation.ok();
        }

        @RequirePOST
        public FormValidation doTestConnection(
                @QueryParameter("artifactoryUrl") final String url,
                @QueryParameter("artifactory.timeout") final String timeout,
                @QueryParameter("artifactory.bypassProxy") final boolean bypassProxy,
                @QueryParameter("useCredentialsPlugin") final boolean useLegacyCredentials,
                @QueryParameter("credentialsId") final String deployerCredentialsId,
                @QueryParameter("username") final String deployerCredentialsUsername,
                @QueryParameter("password") final String deployerCredentialsPassword,
                @QueryParameter("connectionRetry") final int connectionRetry

        ) throws ServletException {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.error("Testing the connection requires 'Administer' permission");
            }
            if (StringUtils.isBlank(url)) {
                return FormValidation.error("Please set a valid Artifactory URL");
            }
            if (connectionRetry < 0) {
                return FormValidation.error("Connection Retries can not be less then 0");
            }

            Credentials credentials = PluginsUtils.credentialsLookup(deployerCredentialsId, null);
            String username = useLegacyCredentials ? credentials.getUsername() : deployerCredentialsUsername;
            String password = useLegacyCredentials ? credentials.getPassword() : deployerCredentialsPassword;

            ArtifactoryBuildInfoClient client;
            if (StringUtils.isNotBlank(username)) {
                client = new ArtifactoryBuildInfoClient(url, username, password, new NullLog());
            } else {
                client = new ArtifactoryBuildInfoClient(url, new NullLog());
            }

            try {
                if (!bypassProxy && Jenkins.getInstance().proxy != null) {
                    client.setProxyConfiguration(RepositoriesUtils.createProxyConfiguration(Jenkins.getInstance().proxy));
                }

                if (StringUtils.isNotBlank(timeout)) {
                    client.setConnectionTimeout(Integer.parseInt(timeout));
                }
                RepositoriesUtils.setRetryParams(connectionRetry, client);

                ArtifactoryVersion version;
                try {
                    version = client.verifyCompatibleArtifactoryVersion();
                } catch (UnsupportedOperationException uoe) {
                    return FormValidation.warning(uoe.getMessage());
                } catch (Exception e) {
                    return FormValidation.error(e.getMessage());
                }
                return FormValidation.ok("Found Artifactory " + version.toString());
            } finally {
                client.close();
            }
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Artifactory Plugin";
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null && jenkins.hasPermission(Jenkins.ADMINISTER)) {
                boolean useCredentialsPlugin = (Boolean) o.get("useCredentialsPlugin");
                Object servers = o.get("artifactoryServer");    // an array or single object
                List<ArtifactoryServer> artifactoryServers;
                if (!JSONNull.getInstance().equals(servers)) {
                    artifactoryServers = req.bindJSONToList(ArtifactoryServer.class, servers);
                } else {
                    artifactoryServers = null;
                }

                if (!isServerIDConfigured(artifactoryServers)) {
                    throw new FormException("Please set the Artifactory server ID.", "ServerID");
                }

                if (isServerDuplicated(artifactoryServers)) {
                    throw new FormException("The Artifactory server ID you have entered is already configured", "Server ID");
                }

                setArtifactoryServers(artifactoryServers);

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

        private void resetServersCredentials() {
            for (ArtifactoryServer server : artifactoryServers) {
                if (server.getResolverCredentialsConfig() != null) {
                    server.getResolverCredentialsConfig().deleteCredentials();
                }
                if (server.getDeployerCredentialsConfig() != null) {
                    server.getDeployerCredentialsConfig().deleteCredentials();
                }
            }
        }

        private void resetJobsCredentials() {
            List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);
            for (AbstractProject job : jobs) {
                if (!(job instanceof BuildableItemWithBuildWrappers)) {
                    continue;
                }
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

        public List<ArtifactoryServer> getArtifactoryServers() {
            return artifactoryServers;
        }

        public boolean getUseCredentialsPlugin() {
            return useCredentialsPlugin;
        }

        // Required by external plugins.
        @SuppressWarnings({"UnusedDeclaration"})
        public void setArtifactoryServers(List<ArtifactoryServer> artifactoryServers) {
            this.artifactoryServers = artifactoryServers;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public void setUseCredentialsPlugin(boolean useCredentialsPlugin) {
            this.useCredentialsPlugin = useCredentialsPlugin;
        }

        private boolean isServerDuplicated(List<ArtifactoryServer> artifactoryServers) {
            Set<String> serversNames = new HashSet<String>();
            if (artifactoryServers == null) {
                return false;
            }
            for (ArtifactoryServer server : artifactoryServers) {
                String name = server.getName();
                if (serversNames.contains(name)) {
                    return true;
                }
                serversNames.add(name);
            }
            return false;
        }

        private boolean isServerIDConfigured(List<ArtifactoryServer> artifactoryServers) {
            if (artifactoryServers == null) {
                return true;
            }
            for (ArtifactoryServer server : artifactoryServers) {
                String name = server.getName();
                if (StringUtils.isBlank(name)) {
                    return false;
                }
            }
            return true;
        }
    }
}
