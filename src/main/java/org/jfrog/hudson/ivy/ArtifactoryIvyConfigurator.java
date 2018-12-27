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

package org.jfrog.hudson.ivy;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ivy.AntIvyBuildWrapper;
import hudson.model.*;
import hudson.remoting.Which;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.listener.ArtifactoryBuildListener;
import org.jfrog.hudson.*;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.*;
import org.jfrog.hudson.util.converters.DeployerResolverOverriderConverter;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * @author Tomer Cohen
 */
public class ArtifactoryIvyConfigurator extends AntIvyBuildWrapper implements DeployerOverrider,
        BuildInfoAwareConfigurator {

    private final CredentialsConfig deployerCredentialsConfig;
    private final IncludesExcludes artifactDeploymentPatterns;
    private final boolean discardBuildArtifacts;
    private final String deploymentProperties;
    private final boolean filterExcludedArtifactsFromBuild;
    private ServerDetails deployerDetails;
    private boolean deployArtifacts;
    private boolean deployBuildInfo;
    private boolean includeEnvVars;
    private IncludesExcludes envVarsPatterns;
    private boolean discardOldBuilds;
    private boolean asyncBuildRetention;
    private final Boolean useMavenPatterns;
    private String ivyPattern;
    private String aggregationBuildStatus;
    private String artifactPattern;
    private boolean enableIssueTrackerIntegration;
    private boolean aggregateBuildIssues;
    private String customBuildName;
    private boolean overrideBuildName;

    /**
     * @deprecated: Use org.jfrog.hudson.ivy.ArtifactoryIvyConfigurator#getDeployerCredentialsConfig()()
     */
    @Deprecated
    private Credentials overridingDeployerCredentials;

    /**
     * @deprecated: The following deprecated variables have corresponding converters to the variables replacing them
     */
    @Deprecated
    private ServerDetails details = null;
    @Deprecated
    private final String matrixParams = null;
    @Deprecated
    private final Boolean notM2Compatible = null;

    @DataBoundConstructor
    public ArtifactoryIvyConfigurator(ServerDetails details, ServerDetails deployerDetails, CredentialsConfig deployerCredentialsConfig,
                                      boolean deployArtifacts, IncludesExcludes artifactDeploymentPatterns, boolean deployBuildInfo,
                                      boolean includeEnvVars, IncludesExcludes envVarsPatterns,
                                      Boolean useMavenPatterns, Boolean notM2Compatible, String ivyPattern,
                                      String artifactPattern, boolean discardOldBuilds, boolean discardBuildArtifacts, boolean asyncBuildRetention,
                                      String matrixParams, String deploymentProperties, boolean enableIssueTrackerIntegration, boolean aggregateBuildIssues,
                                      String aggregationBuildStatus, boolean filterExcludedArtifactsFromBuild,
                                      String customBuildName, boolean overrideBuildName) {
        this.deployerDetails = deployerDetails;
        this.deployArtifacts = deployArtifacts;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.deployBuildInfo = deployBuildInfo;
        this.includeEnvVars = includeEnvVars;
        this.envVarsPatterns = envVarsPatterns;
        this.useMavenPatterns = useMavenPatterns;
        this.ivyPattern = ivyPattern;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.filterExcludedArtifactsFromBuild = filterExcludedArtifactsFromBuild;
        this.artifactPattern = clearApostrophes(artifactPattern);
        this.discardOldBuilds = discardOldBuilds;
        this.discardBuildArtifacts = discardBuildArtifacts;
        this.asyncBuildRetention = asyncBuildRetention;
        this.deploymentProperties = deploymentProperties;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.customBuildName = customBuildName;
        this.overrideBuildName = overrideBuildName;
    }

    /**
     * Clears the extra apostrophes from the start and the end of the string
     */
    private String clearApostrophes(String artifactPattern) {
        return StringUtils.removeEnd(StringUtils.removeStart(artifactPattern, "\""), "\"");
    }

    public ServerDetails getDeployerDetails() {
        return deployerDetails;
    }

    public String getDeploymentProperties() {
        return deploymentProperties;
    }

    public boolean isOverridingDefaultDeployer() {
        return deployerCredentialsConfig != null && deployerCredentialsConfig.isCredentialsProvided();
    }

    public Credentials getOverridingDeployerCredentials() {
        return overridingDeployerCredentials;
    }

    public CredentialsConfig getDeployerCredentialsConfig() {
        return deployerCredentialsConfig;
    }

    public boolean isDiscardBuildArtifacts() {
        return discardBuildArtifacts;
    }

    public boolean isDiscardOldBuilds() {
        return discardOldBuilds;
    }

    public void setDiscardOldBuilds(boolean discardOldBuilds) {
        this.discardOldBuilds = discardOldBuilds;
    }

    public boolean isAsyncBuildRetention() {
        return asyncBuildRetention;
    }

    public String getArtifactPattern() {
        return clearApostrophes(artifactPattern);
    }

    public void setArtifactPattern(String artifactPattern) {
        this.artifactPattern = clearApostrophes(artifactPattern);
    }

    public String getIvyPattern() {
        return ivyPattern;
    }

    public void setIvyPattern(String ivyPattern) {
        this.ivyPattern = ivyPattern;
    }

    public boolean isUseMavenPatterns() {
        return useMavenPatterns;
    }

    public boolean isDeployArtifacts() {
        return deployArtifacts;
    }

    public IncludesExcludes getArtifactDeploymentPatterns() {
        return artifactDeploymentPatterns;
    }

    public boolean isDeployBuildInfo() {
        return deployBuildInfo;
    }

    public boolean isIncludeEnvVars() {
        return includeEnvVars;
    }

    public IncludesExcludes getEnvVarsPatterns() {
        return envVarsPatterns;
    }

    public String getArtifactoryName() {
        return getDeployerDetails() != null ? getDeployerDetails().artifactoryName : null;
    }

    public String getRepositoryKey() {
        return getDeployerDetails() != null ? getDeployerDetails().getDeployReleaseRepository().getRepoKey() : null;
    }

    public String getDefaultPromotionTargetRepository() {
        //Not implemented
        return null;
    }

    public String getArtifactoryUrl() {
        ArtifactoryServer server = getArtifactoryServer();
        return server != null ? server.getUrl() : null;
    }

    public boolean isEnableIssueTrackerIntegration() {
        return enableIssueTrackerIntegration;
    }

    public void setEnableIssueTrackerIntegration(boolean enableIssueTrackerIntegration) {
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
    }

    public boolean isAggregateBuildIssues() {
        return aggregateBuildIssues;
    }

    public void setAggregateBuildIssues(boolean aggregateBuildIssues) {
        this.aggregateBuildIssues = aggregateBuildIssues;
    }

    public String getAggregationBuildStatus() {
        return aggregationBuildStatus;
    }

    public void setAggregationBuildStatus(String aggregationBuildStatus) {
        this.aggregationBuildStatus = aggregationBuildStatus;
    }

    public boolean isFilterExcludedArtifactsFromBuild() {
        return filterExcludedArtifactsFromBuild;
    }

    public String getCustomBuildName() {
        return customBuildName;
    }

    public boolean isOverrideBuildName() {
        return overrideBuildName;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject project) {
        if (isOverrideBuildName()) {
            return ActionableHelper.getArtifactoryProjectAction(getArtifactoryName(), project, getCustomBuildName());
        } else {
            return ActionableHelper.getArtifactoryProjectAction(getArtifactoryName(), project);
        }
    }

    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException {
        String artifactoryPluginVersion = ActionableHelper.getArtifactoryPluginVersion();
        listener.getLogger().println("Jenkins Artifactory Plugin version: " + artifactoryPluginVersion);
        File localDependencyFile = Which.jarFile(ArtifactoryBuildListener.class);
        final FilePath actualDependencyDir =
                PluginDependencyHelper.getActualDependencyDirectory(localDependencyFile, ActionableHelper.getNode(launcher).getRootPath());
        final PublisherContext context = new PublisherContext.Builder().artifactoryServer(getArtifactoryServer())
                .serverDetails(getDeployerDetails()).deployerOverrider(ArtifactoryIvyConfigurator.this)
                .discardOldBuilds(isDiscardOldBuilds())
                .deployArtifacts(isDeployArtifacts()).includesExcludes(getArtifactDeploymentPatterns())
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .includeEnvVars(isIncludeEnvVars()).envVarsPatterns(getEnvVarsPatterns())
                .discardBuildArtifacts(isDiscardBuildArtifacts()).asyncBuildRetention(isAsyncBuildRetention())
                .deploymentProperties(getDeploymentProperties()).artifactsPattern(getArtifactPattern()).ivyPattern(getIvyPattern()).maven2Compatible(isUseMavenPatterns())
                .enableIssueTrackerIntegration(isEnableIssueTrackerIntegration())
                .aggregateBuildIssues(isAggregateBuildIssues()).aggregationBuildStatus(getAggregationBuildStatus())
                .filterExcludedArtifactsFromBuild(isFilterExcludedArtifactsFromBuild())
                .artifactoryPluginVersion(artifactoryPluginVersion)
                .overrideBuildName(isOverrideBuildName())
                .customBuildName(getCustomBuildName())
                .build();
        build.setResult(Result.SUCCESS);
        return new AntIvyBuilderEnvironment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                try {
                    ExtractorUtils.addBuilderInfoArguments(env, build, listener, context, null, build.getWorkspace(), launcher);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getAdditionalArgs() {
                StringBuilder targets = new StringBuilder();
                String actualDependencyDirPath = actualDependencyDir.getRemote();
                actualDependencyDirPath = actualDependencyDirPath.replace('\\', '/');
                actualDependencyDirPath = "\"" + actualDependencyDirPath + "\"";
                targets.append("-lib ").append(actualDependencyDirPath).append(" ");
                targets.append("-listener ").append("org.jfrog.build.extractor.listener.ArtifactoryBuildListener")
                        .append(" ");
                return targets.toString();
            }
        };
    }

    public ArtifactoryServer getArtifactoryServer() {
        return RepositoriesUtils.getArtifactoryServer(getArtifactoryName(), getDescriptor().getArtifactoryServers());
    }

    public List<Repository> getReleaseRepositoryList() {
        return RepositoriesUtils.collectRepositories(getDeployerDetails().getDeployReleaseRepository().getKeyFromSelect());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        private AbstractProject<?, ?> item;

        public DescriptorImpl() {
            super(ArtifactoryIvyConfigurator.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            this.item = item;
            return "hudson.ivy.IvyModuleSet".equals(item.getClass().getName());
        }

        /**
         * This method triggered from the client side by Ajax call.
         * The Element that trig is the "Refresh Repositories" button.
         *
         * @param url                 Artifactory url
         * @param credentialsId       credentials Id if using Credentials plugin
         * @param username            credentials legacy mode username
         * @param password            credentials legacy mode password
         * @param overrideCredentials credentials legacy mode overridden
         * @return {@link org.jfrog.hudson.util.RefreshServerResponse} object that represents the response of the repositories
         */
        @JavaScriptMethod
        public RefreshServerResponse refreshFromArtifactory(String url, String credentialsId, String username, String password, boolean overrideCredentials) {
            CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId, overrideCredentials);
            RefreshServerResponse response = new RefreshServerResponse();
            ArtifactoryServer artifactoryServer = RepositoriesUtils.getArtifactoryServer(url, RepositoriesUtils.getArtifactoryServers());

            try {
                List<String> releaseRepositoryKeysFirst = RepositoriesUtils.getLocalRepositories(url, credentialsConfig,
                        artifactoryServer, item);

                Collections.sort(releaseRepositoryKeysFirst);
                List<Repository> releaseRepositoryList = RepositoriesUtils.createRepositoriesList(releaseRepositoryKeysFirst);
                response.setRepositories(releaseRepositoryList);
                response.setSuccess(true);

            } catch (Exception e) {
                response.setResponseMessage(e.getMessage());
                response.setSuccess(false);
            }

            return response;
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            return PluginsUtils.fillPluginCredentials(project);
        }

        @Override
        public String getDisplayName() {
            return "Publish to Artifactory";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/artifactory/ivy/help-publish.html";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindParameters(this, "ivy");
            save();
            return true;
        }

        public FormValidation doCheckArtifactoryName(@QueryParameter String value) {
            return FormValidations.validateInternetAddress(value);
        }

        /**
         * Returns the list of {@link org.jfrog.hudson.ArtifactoryServer} configured.
         *
         * @return can be empty but never null.
         */
        public List<ArtifactoryServer> getArtifactoryServers() {
            return RepositoriesUtils.getArtifactoryServers();
        }

        public boolean isUseCredentialsPlugin() {
            return PluginsUtils.isUseCredentialsPlugin();
        }

        public boolean isJiraPluginEnabled() {
            return (Jenkins.getInstance().getPlugin("jira") != null);
        }
    }

    /**
     * Page Converter
     */
    public static final class ConverterImpl extends DeployerResolverOverriderConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }
    }
}
