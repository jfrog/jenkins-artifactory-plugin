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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.MavenAbstractArtifactRecord;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.BintrayPublish.BintrayPublishAction;
import org.jfrog.hudson.action.ArtifactoryProjectAction;
import org.jfrog.hudson.maven2.ArtifactsDeployer;
import org.jfrog.hudson.maven2.MavenBuildInfoDeployer;
import org.jfrog.hudson.release.UnifiedPromoteBuildAction;
import org.jfrog.hudson.util.*;
import org.jfrog.hudson.util.converters.DeployerResolverOverriderConverter;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link Publisher} for {@link hudson.maven.MavenModuleSetBuild} to deploy artifacts to Artifactory only after a build
 * is fully succeeded.
 *
 * @author Yossi Shaul
 */

public class ArtifactoryRedeployPublisher extends Recorder implements DeployerOverrider, BuildInfoAwareConfigurator {
    /**
     * Deploy even if the build is unstable (failed tests)
     */
    public final boolean evenIfUnstable;
    /**
     * Repository URL and repository to deploy artifacts to.
     */
    private final ServerDetails details;
    /**
     * If checked (default) deploy maven artifacts
     */
    private final boolean deployArtifacts;
    private final IncludesExcludes artifactDeploymentPatterns;
    private final CredentialsConfig deployerCredentialsConfig;
    /**
     * Include environment variables in the generated build info
     */
    private final boolean includeEnvVars;
    private final IncludesExcludes envVarsPatterns;
    private final boolean runChecks;
    private final String violationRecipients;
    private final boolean includePublishArtifacts;
    private final boolean passIdentifiedDownstream;
    private final String scopes;
    private final boolean licenseAutoDiscovery;
    private final boolean disableLicenseAutoDiscovery;
    private final boolean discardOldBuilds;
    private final boolean discardBuildArtifacts;
    private final String matrixParams;
    private final boolean enableIssueTrackerIntegration;
    private final boolean allowPromotionOfNonStagedBuilds;
    private final boolean allowBintrayPushOfNonStageBuilds;
    private final boolean filterExcludedArtifactsFromBuild;
    private final boolean recordAllDependencies;
    private String defaultPromotionTargetRepository;
    private boolean deployBuildInfo;
    private String aggregationBuildStatus;
    private boolean aggregateBuildIssues;
    private boolean blackDuckRunChecks;
    private String blackDuckAppName;
    private String blackDuckAppVersion;
    private String blackDuckReportRecipients; //csv
    private String blackDuckScopes; //csv
    private boolean blackDuckIncludePublishedArtifacts;
    private boolean autoCreateMissingComponentRequests;
    private boolean autoDiscardStaleComponentRequests;
    private String customBuildName;
    private boolean overrideBuildName;
    /**
     * @deprecated: Use org.jfrog.hudson.ArtifactoryRedeployPublisher#deployBuildInfo
     */
    @Deprecated
    private transient Boolean skipBuildInfoDeploy;
    /**
     * @deprecated: Use org.jfrog.hudson.ArtifactoryRedeployPublisher#getDeployerCredentialsConfig()()
     */
    @Deprecated
    private Credentials overridingDeployerCredentials;
    // NOTE: The following getters are used by jelly. Do not remove them

    @DataBoundConstructor
    public ArtifactoryRedeployPublisher(ServerDetails details, boolean deployArtifacts,
                                        IncludesExcludes artifactDeploymentPatterns, CredentialsConfig deployerCredentialsConfig,
                                        boolean includeEnvVars, IncludesExcludes envVarsPatterns,
                                        boolean deployBuildInfo, boolean evenIfUnstable, boolean runChecks,
                                        String violationRecipients, boolean includePublishArtifacts, String scopes,
                                        boolean disableLicenseAutoDiscovery, boolean discardOldBuilds, boolean passIdentifiedDownstream,
                                        boolean discardBuildArtifacts, String matrixParams, boolean enableIssueTrackerIntegration,
                                        boolean aggregateBuildIssues, String aggregationBuildStatus,
                                        boolean recordAllDependencies, boolean allowPromotionOfNonStagedBuilds,
                                        String defaultPromotionTargetRepository,
                                        boolean allowBintrayPushOfNonStageBuilds,
                                        boolean blackDuckRunChecks, String blackDuckAppName, String blackDuckAppVersion,
                                        String blackDuckReportRecipients, String blackDuckScopes,
                                        boolean blackDuckIncludePublishedArtifacts, boolean autoCreateMissingComponentRequests,
                                        boolean autoDiscardStaleComponentRequests, boolean filterExcludedArtifactsFromBuild,
                                        String customBuildName, boolean overrideBuildName) {
        this.details = details;
        this.deployArtifacts = deployArtifacts;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.includeEnvVars = includeEnvVars;
        this.envVarsPatterns = envVarsPatterns;
        this.evenIfUnstable = evenIfUnstable;
        this.runChecks = runChecks;
        this.violationRecipients = violationRecipients;
        this.includePublishArtifacts = includePublishArtifacts;
        this.scopes = scopes;
        this.disableLicenseAutoDiscovery = disableLicenseAutoDiscovery;
        this.discardOldBuilds = discardOldBuilds;
        this.passIdentifiedDownstream = passIdentifiedDownstream;
        this.discardBuildArtifacts = discardBuildArtifacts;
        this.matrixParams = matrixParams;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.filterExcludedArtifactsFromBuild = filterExcludedArtifactsFromBuild;
        this.licenseAutoDiscovery = !disableLicenseAutoDiscovery;
        this.deployBuildInfo = deployBuildInfo;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.recordAllDependencies = recordAllDependencies;
        this.allowPromotionOfNonStagedBuilds = allowPromotionOfNonStagedBuilds;
        this.defaultPromotionTargetRepository = defaultPromotionTargetRepository;
        this.blackDuckRunChecks = blackDuckRunChecks;
        this.blackDuckAppName = blackDuckAppName;
        this.blackDuckAppVersion = blackDuckAppVersion;
        this.blackDuckReportRecipients = blackDuckReportRecipients;
        this.blackDuckScopes = blackDuckScopes;
        this.blackDuckIncludePublishedArtifacts = blackDuckIncludePublishedArtifacts;
        this.autoCreateMissingComponentRequests = autoCreateMissingComponentRequests;
        this.autoDiscardStaleComponentRequests = autoDiscardStaleComponentRequests;
        this.allowBintrayPushOfNonStageBuilds = allowBintrayPushOfNonStageBuilds;
        this.customBuildName = customBuildName;
        this.overrideBuildName = overrideBuildName;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public boolean isDeployArtifacts() {
        return deployArtifacts;
    }

    public String getMatrixParams() {
        return matrixParams;
    }

    public ServerDetails getDetails() {
        return details;
    }

    public IncludesExcludes getArtifactDeploymentPatterns() {
        return artifactDeploymentPatterns;
    }

    public boolean isDiscardOldBuilds() {
        return discardOldBuilds;
    }

    public boolean isDiscardBuildArtifacts() {
        return discardBuildArtifacts;
    }

    public boolean isPassIdentifiedDownstream() {
        return passIdentifiedDownstream;
    }

    public CredentialsConfig getDeployerCredentialsConfig() {
        return deployerCredentialsConfig;
    }

    public boolean isOverridingDefaultDeployer() {
        return deployerCredentialsConfig != null && deployerCredentialsConfig.isCredentialsProvided();
    }

    public Credentials getOverridingDeployerCredentials() {
        return overridingDeployerCredentials;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public boolean isDeployBuildInfo() {
        return deployBuildInfo;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public boolean isEvenIfUnstable() {
        return evenIfUnstable;
    }

    public boolean isIncludePublishArtifacts() {
        return includePublishArtifacts;
    }

    public boolean isIncludeEnvVars() {
        return includeEnvVars;
    }

    public IncludesExcludes getEnvVarsPatterns() {
        return envVarsPatterns;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public boolean isDisableLicenseAutoDiscovery() {
        return disableLicenseAutoDiscovery;
    }

    public boolean isLicenseAutoDiscovery() {
        return licenseAutoDiscovery;
    }

    public boolean isRunChecks() {
        return runChecks;
    }

    public String getScopes() {
        return scopes;
    }

    public String getViolationRecipients() {
        return violationRecipients;
    }

    public String getArtifactoryName() {
        return details != null ? details.artifactoryName : null;
    }

    public String getArtifactoryUrl() {
        ArtifactoryServer server = getArtifactoryServer();
        return server != null ? server.getUrl() : null;
    }

    /**
     * @return The release versions deployment repository.
     */
    public String getRepositoryKey() {
        return details != null ?
                details.getDeployReleaseRepositoryKey() : null;
    }

    /**
     * @return The snapshots deployment repository. If not defined the releases deployment repository will be returned
     */
    public String getSnapshotsRepositoryKey() {
        return details != null ?
                details.getDeploySnapshotRepositoryKey() : null;
    }

    public String getUserPluginKey() {
        return details != null ? details.getUserPluginKey() : null;
    }

    public boolean isEnableIssueTrackerIntegration() {
        return enableIssueTrackerIntegration;
    }

    public boolean isAggregateBuildIssues() {
        return aggregateBuildIssues;
    }

    public String getAggregationBuildStatus() {
        return aggregationBuildStatus;
    }

    public boolean isAllowPromotionOfNonStagedBuilds() {
        return allowPromotionOfNonStagedBuilds;
    }

    public String getDefaultPromotionTargetRepository() {
        return defaultPromotionTargetRepository;
    }

    public void setDefaultPromotionTargetRepository(String defaultPromotionTargetRepository) {
        this.defaultPromotionTargetRepository = defaultPromotionTargetRepository;
    }

    public boolean isAllowBintrayPushOfNonStageBuilds() {
        return allowBintrayPushOfNonStageBuilds;
    }

    public boolean isRecordAllDependencies() {
        return recordAllDependencies;
    }

    public boolean isBlackDuckRunChecks() {
        return blackDuckRunChecks;
    }

    public String getBlackDuckAppName() {
        return blackDuckAppName;
    }

    public String getBlackDuckAppVersion() {
        return blackDuckAppVersion;
    }

    public String getBlackDuckReportRecipients() {
        return blackDuckReportRecipients;
    }

    public String getBlackDuckScopes() {
        return blackDuckScopes;
    }

    public boolean isBlackDuckIncludePublishedArtifacts() {
        return blackDuckIncludePublishedArtifacts;
    }

    public boolean isAutoCreateMissingComponentRequests() {
        return autoCreateMissingComponentRequests;
    }

    public boolean isAutoDiscardStaleComponentRequests() {
        return autoDiscardStaleComponentRequests;
    }

    public boolean isFilterExcludedArtifactsFromBuild() {
        return filterExcludedArtifactsFromBuild;
    }

    public boolean isApplicable(AbstractBuild build) {
        return !isBuildFromM2ReleasePlugin(build);
    }

    public String getCustomBuildName() {
        return customBuildName;
    }

    public boolean isOverrideBuildName() {
        return overrideBuildName;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        if (details != null) {
            if (isOverrideBuildName()) {
                return new ArtifactoryProjectAction(details.getArtifactoryName(), getCustomBuildName());
            }
            return new ArtifactoryProjectAction(details.getArtifactoryName(), project);
        }
        return null;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        if (build.getResult().isWorseThan(getTreshold())) {
            return true;    // build failed. Don't publish
        }
        if (isBuildFromM2ReleasePlugin(build)) {
            listener.getLogger().append("M2 Release build, not uploading artifacts to Artifactory. ");
            return true;
        }

        String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(ArtifactoryRedeployPublisher.this, build);
        // The following if statement Checks if the build job uses Maven 3:
        if (isExtractorUsed(build.getEnvironment(listener))) {
            if (deployBuildInfo) {
                //Add build info icon to Jenkins job
                addJobActions(build, buildName);
            }
            return true;
        }

        // The following code is executed only if we are not using Maven 3, but Maven 2:
        if (!(build instanceof MavenModuleSetBuild)) {
            listener.getLogger().format("Non maven build type: %s", build.getClass()).println();
            build.setResult(Result.FAILURE);
            return true;
        }
        MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
        if (getArtifactoryServer() == null) {
            listener.getLogger().format("No Artifactory server configured for %s. " +
                    "Please check your configuration.", getArtifactoryName()).println();
            build.setResult(Result.FAILURE);
            return true;
        }

        List<MavenAbstractArtifactRecord> mars = getArtifactRecordActions(mavenBuild);
        if (mars.isEmpty()) {
            listener.getLogger().println("No artifacts are recorded. Is this a Maven project?");
            build.setResult(Result.FAILURE);
            return true;
        }

        ArtifactoryServer server = getArtifactoryServer();
        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(this, server);
        ArtifactoryBuildInfoClient client = server.createArtifactoryClient(preferredDeployer.provideUsername(((MavenModuleSetBuild) build).getProject()),
                preferredDeployer.providePassword(((MavenModuleSetBuild) build).getProject()), server.createProxyConfiguration(Jenkins.getInstance().proxy));
        try {
            verifySupportedArtifactoryVersion(client);
            if (deployArtifacts) {
                new ArtifactsDeployer(this, client, mavenBuild, listener).deploy();
            }
            if (deployBuildInfo) {
                new MavenBuildInfoDeployer(this, client, mavenBuild, listener).deploy();
                // add the result action (prefer always the same index)
                addJobActions(build, buildName);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace(listener.error(e.getMessage()));
        } finally {
            client.shutdown();
        }
        // failed
        build.setResult(Result.FAILURE);
        return true;
    }

    private void addJobActions(AbstractBuild build, String buildName) {
        build.getActions().add(0, new BuildInfoResultAction(getArtifactoryUrl(), build, buildName));
        if (isAllowPromotionOfNonStagedBuilds()) {
            build.getActions().add(new UnifiedPromoteBuildAction<ArtifactoryRedeployPublisher>(build, this));
        }
        // Checks if Push to Bintray is disabled.
        if (PluginsUtils.isPushToBintrayEnabled()) {
            if (isAllowBintrayPushOfNonStageBuilds()) {
                build.getActions().add(new BintrayPublishAction<ArtifactoryRedeployPublisher>(build, this));
            }
        }
    }

    private boolean isBuildFromM2ReleasePlugin(AbstractBuild<?, ?> build) {
        List<Cause> causes = build.getCauses();
        return !causes.isEmpty() && Iterables.any(causes, new Predicate<Cause>() {
            public boolean apply(Cause input) {
                return "org.jvnet.hudson.plugins.m2release.ReleaseCause".equals(input.getClass().getName());
            }
        });
    }

    private boolean isM2Build(AbstractBuild<?, ?> build) {
        return build.getClass().getName().contains("MavenModuleSetBuild")
                && ((MavenModuleSetBuild) build).getMavenVersionUsed().startsWith("2");
    }

    private boolean isExtractorUsed(EnvVars env) {
        return Boolean.parseBoolean(env.get(ExtractorUtils.EXTRACTOR_USED));
    }

    private void verifySupportedArtifactoryVersion(ArtifactoryBuildInfoClient client) throws Exception {
        // get the version of artifactory, if it is an unsupported version, an UnsupportedOperationException
        // will be thrown, and no deployment will commence.
        client.verifyCompatibleArtifactoryVersion();
    }

    protected List<MavenAbstractArtifactRecord> getArtifactRecordActions(MavenModuleSetBuild build) {
        List<MavenAbstractArtifactRecord> actions = Lists.newArrayList();
        for (MavenBuild moduleBuild : build.getModuleLastBuilds().values()) {
            MavenAbstractArtifactRecord action = moduleBuild.getAction(MavenAbstractArtifactRecord.class);
            if (action != null) {
                actions.add(action);
            }
        }
        return actions;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public ArtifactoryServer getArtifactoryServer() {
        return RepositoriesUtils.getArtifactoryServer(getArtifactoryName(), getDescriptor().getArtifactoryServers());
    }

    private Result getTreshold() {
        if (evenIfUnstable) {
            return Result.UNSTABLE;
        } else {
            return Result.SUCCESS;
        }
    }

    public List<Repository> getReleaseRepositoryList() {
        return RepositoriesUtils.collectRepositories(details.getDeployReleaseRepositoryKey());
    }

    public List<Repository> getSnapshotRepositoryList() {
        return RepositoriesUtils.collectRepositories(details.getDeploySnapshotRepositoryKey());
    }

    public PluginSettings getSelectedStagingPlugin() throws Exception {
        return details.getStagingPlugin();
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private Item item;

        public DescriptorImpl() {
            super(ArtifactoryRedeployPublisher.class);
            load();
        }

        private List<Repository> refreshRepositories(ArtifactoryServer artifactoryServer, CredentialsConfig credentialsConfig)
                throws IOException {
            List<String> repositoriesKeys = RepositoriesUtils.getLocalRepositories(artifactoryServer.getUrl(),
                    credentialsConfig, artifactoryServer, item);
            List<Repository> releaseRepositories = RepositoriesUtils.createRepositoriesList(repositoriesKeys);
            Collections.sort(releaseRepositories);
            return releaseRepositories;
        }

        private List<PluginSettings> refreshUserPlugins(ArtifactoryServer artifactoryServer, final CredentialsConfig credentialsConfig) {
            List<UserPluginInfo> pluginInfoList = artifactoryServer.getStagingUserPluginInfo(new DeployerOverrider() {
                public boolean isOverridingDefaultDeployer() {
                    return credentialsConfig != null && credentialsConfig.isCredentialsProvided();
                }

                public Credentials getOverridingDeployerCredentials() {
                    return credentialsConfig.getCredentials(item);
                }

                public CredentialsConfig getDeployerCredentialsConfig() {
                    return credentialsConfig;
                }
            }, item);

            ArrayList<PluginSettings> list = new ArrayList<PluginSettings>(pluginInfoList.size());
            for (UserPluginInfo p : pluginInfoList) {
                Map<String, String> paramsMap = Maps.newHashMap();
                List<UserPluginInfoParam> params = p.getPluginParams();
                for (UserPluginInfoParam param : params) {
                    paramsMap.put(((String) param.getKey()), ((String) param.getDefaultValue()));
                }
                PluginSettings plugin = new PluginSettings(p.getPluginName(), paramsMap);
                list.add(plugin);
            }

            return list;
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            this.item = project;
            return PluginsUtils.fillPluginCredentials(project);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType == MavenModuleSet.class;
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
         * @return {@link RefreshServerResponse} object that represents the response of the repositories
         */
        @JavaScriptMethod
        public RefreshServerResponse refreshFromArtifactory(String url, String credentialsId, String username, String password, boolean overrideCredentials) {
            RefreshServerResponse response = new RefreshServerResponse();
            CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId, overrideCredentials);

            try {
                ArtifactoryServer artifactoryServer = RepositoriesUtils.getArtifactoryServer(
                        url, getArtifactoryServers()
                );
                List<Repository> releaseRepositories = refreshRepositories(artifactoryServer, credentialsConfig);
                List<PluginSettings> userPluginKeys = refreshUserPlugins(artifactoryServer, credentialsConfig);

                response.setRepositories(releaseRepositories);
                response.setUserPlugins(userPluginKeys);
                response.setSuccess(true);
            } catch (Exception e) {
                e.printStackTrace();
                response.setResponseMessage(e.getMessage());
                response.setSuccess(false);
            }
            return response;
        }

        @Override
        public ArtifactoryRedeployPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(ArtifactoryRedeployPublisher.class, formData);
        }

        @Override
        public String getDisplayName() {
            return "Deploy artifacts to Artifactory";
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public FormValidation doCheckViolationRecipients(@QueryParameter String value) {
            return FormValidations.validateEmails(value);
        }

        /**
         * Returns the list of {@link ArtifactoryServer} configured.
         *
         * @return can be empty but never null.
         */
        public List<ArtifactoryServer> getArtifactoryServers() {
            return RepositoriesUtils.getArtifactoryServers();
        }

        public boolean isJiraPluginEnabled() {
            return (Jenkins.getInstance().getPlugin("jira") != null);
        }

        public boolean isUseCredentialsPlugin() {
            return PluginsUtils.isUseCredentialsPlugin();
        }
        public boolean isPushToBintrayEnabled() {
            return PluginsUtils.isPushToBintrayEnabled();
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
