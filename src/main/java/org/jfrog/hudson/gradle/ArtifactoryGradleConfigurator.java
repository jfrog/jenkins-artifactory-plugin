/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.hudson.gradle;

import com.google.common.collect.Iterables;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.plugins.gradle.Gradle;
import hudson.tasks.BuildWrapper;
import hudson.util.ListBoxModel;
import hudson.util.XStream2;
import org.apache.commons.lang.StringUtils;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;
import org.jfrog.hudson.*;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.action.ArtifactoryProjectAction;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.release.gradle.BaseGradleReleaseAction;
import org.jfrog.hudson.release.gradle.GradleReleaseAction;
import org.jfrog.hudson.release.gradle.GradleReleaseApiAction;
import org.jfrog.hudson.release.gradle.GradleReleaseWrapper;
import org.jfrog.hudson.release.promotion.UnifiedPromoteBuildAction;
import org.jfrog.hudson.util.*;
import org.jfrog.hudson.util.converters.DeployerResolverOverriderConverter;
import org.jfrog.hudson.util.plugins.MultiConfigurationUtils;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Gradle-Artifactory plugin configuration, allows to add the server details, deployment username/password, as well as
 * flags to deploy ivy, maven, and artifacts, as well as specifications of the location of the remote plugin (.gradle)
 * groovy script.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryGradleConfigurator extends BuildWrapper implements DeployerOverrider, ResolverOverrider,
        BuildInfoAwareConfigurator, MultiConfigurationAware {
    public final boolean deployMaven;
    public final boolean deployIvy;
    public final String remotePluginLocation;
    public final boolean deployBuildInfo;
    public final boolean includeEnvVars;
    private final CredentialsConfig deployerCredentialsConfig;
    private final CredentialsConfig resolverCredentialsConfig;
    private final String ivyPattern;
    private final boolean enableIssueTrackerIntegration;
    private final boolean aggregateBuildIssues;
    private final String artifactPattern;
    private final Boolean useMavenPatterns;
    private final IncludesExcludes artifactDeploymentPatterns;
    private final boolean discardOldBuilds;
    private final boolean passIdentifiedDownstream;
    private final GradleReleaseWrapper releaseWrapper;
    private final boolean discardBuildArtifacts;
    private final boolean asyncBuildRetention;
    private final String deploymentProperties;
    private final Boolean useArtifactoryGradlePlugin;
    private final boolean allowPromotionOfNonStagedBuilds;
    private final boolean filterExcludedArtifactsFromBuild;
    private final ServerDetails resolverDetails;
    private String defaultPromotionTargetRepository;
    private ServerDetails deployerDetails;
    private boolean deployArtifacts;
    private IncludesExcludes envVarsPatterns;
    private String aggregationBuildStatus;
    private String artifactoryCombinationFilter;
    private String customBuildName;
    private boolean overrideBuildName;

    /**
     * @deprecated: Use org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator#getDeployerCredentialsConfig()
     */
    @Deprecated
    private Credentials overridingDeployerCredentials;
    /**
     * @deprecated: Use org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator#getResolverCredentialsConfig()
     */
    @Deprecated
    private Credentials overridingResolverCredentials;

    /**
     * @deprecated: The following deprecated variables have corresponding converters to the variables replacing them
     */
    @Deprecated
    private ServerDetails details = null;
    @Deprecated
    private final String matrixParams = null;
    @Deprecated
    private final Boolean notM2Compatible = null;
    @Deprecated
    private final Boolean skipInjectInitScript = null;

    @DataBoundConstructor
    public ArtifactoryGradleConfigurator(ServerDetails details, ServerDetails deployerDetails, ServerDetails resolverDetails,
                                         CredentialsConfig deployerCredentialsConfig, CredentialsConfig resolverCredentialsConfig,
                                         boolean deployMaven, boolean deployIvy, boolean deployArtifacts,
                                         String remotePluginLocation, boolean includeEnvVars,
                                         IncludesExcludes envVarsPatterns, boolean deployBuildInfo,
                                         String ivyPattern, String artifactPattern,
                                         Boolean useMavenPatterns, Boolean notM2Compatible, IncludesExcludes artifactDeploymentPatterns,
                                         boolean discardOldBuilds, boolean passIdentifiedDownstream,
                                         GradleReleaseWrapper releaseWrapper, boolean discardBuildArtifacts, boolean asyncBuildRetention,
                                         String matrixParams, String deploymentProperties, Boolean skipInjectInitScript, Boolean useArtifactoryGradlePlugin,
                                         boolean enableIssueTrackerIntegration, boolean aggregateBuildIssues,
                                         String aggregationBuildStatus, boolean allowPromotionOfNonStagedBuilds,
                                         String defaultPromotionTargetRepository,
                                         boolean filterExcludedArtifactsFromBuild, String artifactoryCombinationFilter,
                                         String customBuildName, boolean overrideBuildName) {
        this.deployerDetails = deployerDetails;
        this.resolverDetails = resolverDetails;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.resolverCredentialsConfig = resolverCredentialsConfig;
        this.deployMaven = deployMaven;
        this.deployIvy = deployIvy;
        this.deployArtifacts = deployArtifacts;
        this.remotePluginLocation = remotePluginLocation;
        this.includeEnvVars = includeEnvVars;
        this.envVarsPatterns = envVarsPatterns;
        this.deployBuildInfo = deployBuildInfo;
        this.ivyPattern = ivyPattern;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.filterExcludedArtifactsFromBuild = filterExcludedArtifactsFromBuild;
        this.artifactPattern = cleanString(artifactPattern);
        this.useMavenPatterns = useMavenPatterns;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.discardOldBuilds = discardOldBuilds;
        this.passIdentifiedDownstream = passIdentifiedDownstream;
        this.releaseWrapper = releaseWrapper;
        this.asyncBuildRetention = asyncBuildRetention;
        this.discardBuildArtifacts = discardBuildArtifacts;
        this.deploymentProperties = deploymentProperties;
        this.useArtifactoryGradlePlugin = useArtifactoryGradlePlugin;
        this.allowPromotionOfNonStagedBuilds = allowPromotionOfNonStagedBuilds;
        this.defaultPromotionTargetRepository = defaultPromotionTargetRepository;
        this.artifactoryCombinationFilter = artifactoryCombinationFilter;
        this.customBuildName = customBuildName;
        this.overrideBuildName = overrideBuildName;
    }

    public GradleReleaseWrapper getReleaseWrapper() {
        return releaseWrapper;
    }

    public ServerDetails getDeployerDetails() {
        return deployerDetails;
    }

    public ServerDetails getResolverDetails() {
        return resolverDetails;
    }

    public String getDeploymentProperties() {
        return deploymentProperties;
    }

    public boolean isPassIdentifiedDownstream() {
        return passIdentifiedDownstream;
    }

    public boolean isDiscardBuildArtifacts() {
        return discardBuildArtifacts;
    }

    public boolean isDiscardOldBuilds() {
        return discardOldBuilds;
    }

    public boolean isAsyncBuildRetention() {
        return asyncBuildRetention;
    }

    public boolean isUseArtifactoryGradlePlugin() {
        if (useArtifactoryGradlePlugin != null) {
            return useArtifactoryGradlePlugin;
        }
        return false;
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

    public String getArtifactPattern() {
        return cleanString(artifactPattern);
    }

    public String getIvyPattern() {
        return ivyPattern;
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

    public String getRepositoryKey() {
        return getDeployerDetails() != null ? getDeployerDetails().getDeployReleaseRepository().getRepoKey() : null;
    }

    public String getUserPluginKey() {
        return getDeployerDetails() != null ? getDeployerDetails().getUserPluginKey() : null;
    }

    public String getDownloadReleaseRepositoryKey() {
        return getDeployerDetails() != null ? getDeployerDetails().getResolveReleaseRepository().getRepoKey() : null;
    }

    public String getArtifactoryName() {
        return getDeployerDetails() != null ? getDeployerDetails().artifactoryName : null;
    }

    public String getArtifactoryResolverName() {
        return resolverDetails != null ? resolverDetails.artifactoryName : null;
    }

    public String getArtifactoryUrl() {
        ArtifactoryServer server = getArtifactoryServer();
        return server != null ? server.getUrl() : null;
    }

    public boolean isDeployArtifacts() {
        return deployArtifacts;
    }

    public boolean isDeployMaven() {
        return deployMaven;
    }

    public boolean isDeployIvy() {
        return deployIvy;
    }

    public boolean isUseMavenPatterns() {
        return useMavenPatterns;
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

    public boolean isFilterExcludedArtifactsFromBuild() {
        return filterExcludedArtifactsFromBuild;
    }

    public String getArtifactoryCombinationFilter() {
        return artifactoryCombinationFilter;
    }

    public boolean isMultiConfProject() {
        return getDescriptor().isMultiConfProject();
    }

    public String getCustomBuildName() {
        return customBuildName;
    }

    public boolean isOverrideBuildName() {
        return overrideBuildName;
    }

    private String cleanString(String artifactPattern) {
        return StringUtils.removeEnd(StringUtils.removeStart(artifactPattern, "\""), "\"");
    }

    @Override
    public Action getProjectAction(AbstractProject job) {
        return super.getProjectAction(job);
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject project) {
        List<ArtifactoryProjectAction> action;
        if (isOverrideBuildName()) {
            action = ActionableHelper.getArtifactoryProjectAction(getArtifactoryName(), project, getCustomBuildName());
        } else {
            action = ActionableHelper.getArtifactoryProjectAction(getArtifactoryName(), project);
        }
        if (getReleaseWrapper() != null) {
            List<Action> actions = new ArrayList<Action>();
            actions.addAll(action);
            actions.add(new GradleReleaseAction(project));
            actions.add(new GradleReleaseApiAction(project));
            return actions;
        }
        return action;
    }

    @Override
    public Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException {
        final PrintStream log = listener.getLogger();
        log.println("Jenkins Artifactory Plugin version: " + ActionableHelper.getArtifactoryPluginVersion());
        PublisherContext.Builder publisherBuilder = getBuilder();
        RepositoriesUtils.validateServerConfig(build, listener, getArtifactoryServer(), getArtifactoryUrl());

        int totalBuilds = 1;

        if (isMultiConfProject(build)) {
            totalBuilds = ((MatrixProject) build.getParent().getParent()).getActiveConfigurations().size();
            if (isDeployArtifacts()) {
                if (StringUtils.isBlank(getArtifactoryCombinationFilter())) {
                    String error = "The field \"Combination Matches\" is empty, but is defined as mandatory!";
                    log.println(error);
                    build.setResult(Result.FAILURE);
                    throw new IllegalArgumentException(error);
                }
                boolean isFiltered = MultiConfigurationUtils.isfiltrated(build, getArtifactoryCombinationFilter());
                if (isFiltered) {
                    publisherBuilder.skipBuildInfoDeploy(true).deployArtifacts(false);
                }
            }
        }

        if (isRelease(build)) {
            releaseWrapper.setUp(build, launcher, listener);
        }

        final Gradle gradleBuild = getLastGradleBuild(build.getProject());
        if (gradleBuild != null) {
            // The ConcurrentBuildSetupSync helper class is used to make sure that the code
            // inside its setUp() method is invoked by only one job in this build
            // (matrix project builds include more that one job) and that all other jobs
            // wait till the seUup() method finishes.
            new ConcurrentJobsHelper.ConcurrentBuildSetupSync(build, totalBuilds) {
                @Override
                public void setUp() {
                    // Obtain the current build and use it to store the configured switches and tasks.
                    // We store them because we override them during the build and we'll need
                    // their original values at the tear down stage so that they can be restored.
                    ConcurrentJobsHelper.ConcurrentBuild concurrentBuild = ConcurrentJobsHelper.getConcurrentBuild(build);

                    // Remove the Artifactory Plugin additional switches and tasks,
                    // in case they are included in the targets string:
                    String switches = gradleBuild.getSwitches() != null ? gradleBuild.getSwitches().replace("${ARTIFACTORY_INIT_SCRIPT}", "") : "";
                    String tasks = gradleBuild.getTasks() != null ? gradleBuild.getTasks().replace("${ARTIFACTORY_TASKS}", "") : "";

                    concurrentBuild.putParam("switches", switches);
                    concurrentBuild.putParam("tasks", tasks);

                    // Override the build switches:
                    if (!isUseArtifactoryGradlePlugin()) {
                        setTargetsField(gradleBuild, "switches", switches + " " + "${ARTIFACTORY_INIT_SCRIPT}");
                    }
                    // Override the build tasks:
                    if (!StringUtils.contains(gradleBuild.getTasks(), ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME)) {
                        // In case we specified "alternative goals" in the release view we should override the build goals
                        if (isRelease(build) && StringUtils.isNotBlank(releaseWrapper.getAlternativeTasks())) {
                            tasks = "${ARTIFACTORY_TASKS}";
                        } else {
                            tasks += " ${ARTIFACTORY_TASKS}";
                        }
                        setTargetsField(gradleBuild, "tasks", tasks);
                    }
                }
            };
        } else {
            log.println("[Warning] No Gradle build configured");
        }

        final PublisherContext.Builder finalPublisherBuilder = publisherBuilder;

        return new Environment() {
            String initScriptPath;

            @Override
            public void buildEnvVars(Map<String, String> env) {
                GradleInitScriptWriter writer = new GradleInitScriptWriter(ActionableHelper.getNode(launcher).getRootPath());
                FilePath workspace = build.getWorkspace();
                FilePath initScript;
                try {
                    initScript = workspace.createTextTempFile("init-artifactory", "gradle",
                            writer.generateInitScript(new EnvVars(env)), false);
                    ActionableHelper.deleteFilePathOnExit(initScript);
                    initScriptPath = initScript.getRemote();
                    initScriptPath = initScriptPath.replace('\\', '/');
                    env.put("ARTIFACTORY_INIT_SCRIPT", " --init-script " + initScriptPath);

                } catch (Exception e) {
                    log.println("Error occurred while writing Gradle Init Script: " + e.getMessage());
                    build.setResult(Result.FAILURE);
                }

                String tasks = StringUtils.EMPTY;
                if (isRelease(build)) {
                    String alternativeGoals = releaseWrapper.getAlternativeTasks();
                    if (StringUtils.isNotBlank(alternativeGoals)) {
                        tasks = alternativeGoals;
                    }
                }
                env.put("ARTIFACTORY_TASKS", tasks + " " + ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME);

                ServerDetails serverDetails = getDeployerDetails();
                serverDetails = releaseActionOverride(env, serverDetails);
                finalPublisherBuilder.serverDetails(serverDetails);

                ServerDetails resolverServerDetails = getResolverDetails();
                ResolverContext resolverContext = null;
                if (StringUtils.isNotBlank(resolverServerDetails.getResolveReleaseRepository().getRepoKey())) {
                    CredentialsConfig resolverCredentials = CredentialManager.getPreferredResolver(
                            ArtifactoryGradleConfigurator.this, getArtifactoryServer());
                    resolverContext = new ResolverContext(getArtifactoryResolverServer(), resolverServerDetails,
                            resolverCredentials.provideCredentials(build.getProject()), ArtifactoryGradleConfigurator.this);
                }

                try {
                    ExtractorUtils.addBuilderInfoArguments(env, build, listener,
                            finalPublisherBuilder.build(), resolverContext, build.getWorkspace(), launcher);
                } catch (Exception e) {
                    log.println(e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean tearDown(final AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                boolean success = false;
                boolean releaseSuccess = true;
                Result result = build.getResult();

                if (isRelease(build)) {
                    releaseSuccess = releaseWrapper.tearDown(build, listener);
                }
                if (gradleBuild != null) {
                    // The ConcurrentBuildTearDownSync helper class is used to make sure that the code
                    // inside its tearDown() method is invoked by only one job in this build
                    // (matrix project builds include more that one job) and that this
                    // job is the last one running.
                    new ConcurrentJobsHelper.ConcurrentBuildTearDownSync(build, result) {
                        @Override
                        public void tearDown() {
                            // Restore the original switches and tasks of this build (we overrided their
                            // values in the setUp stage):
                            ConcurrentJobsHelper.ConcurrentBuild concurrentBuild = ConcurrentJobsHelper.getConcurrentBuild(build);
                            String switches = concurrentBuild.getParam("switches");
                            String tasks = concurrentBuild.getParam("tasks");
                            switches = switches.replace("${ARTIFACTORY_INIT_SCRIPT}", "");
                            tasks = tasks.replace("${ARTIFACTORY_TASKS}", "");
                            setTargetsField(gradleBuild, "switches", switches);
                            setTargetsField(gradleBuild, "tasks", tasks);
                            try {
                                ActionableHelper.deleteFilePath(build.getWorkspace(), initScriptPath);
                            } catch (IOException e) {
                                log.println(e.getStackTrace());
                            }
                        }
                    };
                }
                if (result != null && result.isBetterOrEqualTo(Result.SUCCESS)) {
                    if (isDeployBuildInfo()) {
                        String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(ArtifactoryGradleConfigurator.this, build);
                        build.getActions().add(new BuildInfoResultAction(getArtifactoryUrl(), build, buildName));
                        ArtifactoryGradleConfigurator configurator =
                                ActionableHelper.getBuildWrapper(build.getProject(),
                                        ArtifactoryGradleConfigurator.class);
                        if (configurator != null) {
                            if (isAllowPromotionOfNonStagedBuilds()) {
                                build.getActions()
                                        .add(new UnifiedPromoteBuildAction(build, ArtifactoryGradleConfigurator.this));
                            }
                        }
                    }
                    success = true;
                }
                // Aborted action by the user:
                if (Result.ABORTED.equals(result)) {
                    ConcurrentJobsHelper.removeConcurrentBuildJob(build);
                }

                return success && releaseSuccess;
            }

            private ServerDetails releaseActionOverride(Map<String, String> env, ServerDetails serverDetails) {
                ReleaseAction releaseAction = ActionableHelper.getLatestAction(build, ReleaseAction.class);
                if (releaseAction != null) {
                    releaseAction.addVars(env);
                    String stagingRepository = releaseAction.getStagingRepositoryKey();
                    if (StringUtils.isBlank(stagingRepository)) {
                        stagingRepository = getRepositoryKey();
                    }
                    serverDetails = new ServerDetails(
                            serverDetails.artifactoryName,
                            serverDetails.getArtifactoryUrl(),
                            new RepositoryConf(stagingRepository, stagingRepository, false),
                            serverDetails.getDeploySnapshotRepository(),
                            serverDetails.getResolveReleaseRepository(),
                            serverDetails.getResolveSnapshotRepository());
                }
                return serverDetails;
            }
        };
    }

    private PublisherContext.Builder getBuilder() {
        return new PublisherContext.Builder()
                .artifactoryServer(getArtifactoryServer())
                .deployerOverrider(ArtifactoryGradleConfigurator.this)
                .discardOldBuilds(isDiscardOldBuilds())
                .deployArtifacts(isDeployArtifacts()).includesExcludes(getArtifactDeploymentPatterns())
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .includeEnvVars(isIncludeEnvVars()).envVarsPatterns(getEnvVarsPatterns())
                .discardBuildArtifacts(isDiscardBuildArtifacts()).asyncBuildRetention(isAsyncBuildRetention())
                .deploymentProperties(getDeploymentProperties()).artifactsPattern(getArtifactPattern()).ivyPattern(getIvyPattern())
                .deployIvy(isDeployIvy()).deployMaven(isDeployMaven()).maven2Compatible(isUseMavenPatterns())
                .enableIssueTrackerIntegration(isEnableIssueTrackerIntegration())
                .aggregateBuildIssues(isAggregateBuildIssues())
                .aggregationBuildStatus(getAggregationBuildStatus())
                .filterExcludedArtifactsFromBuild(isFilterExcludedArtifactsFromBuild())
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion())
                .overrideBuildName(isOverrideBuildName())
                .customBuildName(getCustomBuildName());
    }

    public boolean isRelease(AbstractBuild build) {
        boolean actionExists = build.getAction(GradleReleaseAction.class) != null ||
                build.getAction(GradleReleaseApiAction.class) != null;
        return getReleaseWrapper() != null && actionExists;
    }

    private Gradle getLastGradleBuild(AbstractProject project) {
        if (project instanceof Project) {
            List<Gradle> gradles = ActionableHelper.getBuilder((Project) project, Gradle.class);
            return Iterables.getLast(gradles, null);
        }
        return null;
    }

    private void setTargetsField(Gradle builder, String fieldName, String value) {
        try {
            Field targetsField = builder.getClass().getDeclaredField(fieldName);
            targetsField.setAccessible(true);
            targetsField.set(builder, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArtifactoryServer getArtifactoryServer() {
        return RepositoriesUtils.getArtifactoryServer(getArtifactoryName(), getDescriptor().getArtifactoryServers());
    }

    public ArtifactoryServer getArtifactoryResolverServer() {
        return RepositoriesUtils.getArtifactoryServer(getArtifactoryResolverName(),
                getDescriptor().getArtifactoryServers());
    }

    public List<Repository> getReleaseRepositories() {
        return RepositoriesUtils.collectRepositories(getDeployerDetails().getDeployReleaseRepository().getKeyFromSelect());
    }

    public List<VirtualRepository> getVirtualRepositories() {
        return RepositoriesUtils.collectVirtualRepositories(null, resolverDetails.getResolveSnapshotRepository().getKeyFromSelect());
    }

    public boolean isOverridingDefaultResolver() {
        return resolverCredentialsConfig != null && resolverCredentialsConfig.isCredentialsProvided();
    }

    public Credentials getOverridingResolverCredentials() {
        return overridingResolverCredentials;
    }

    public CredentialsConfig getResolverCredentialsConfig() {
        return resolverCredentialsConfig;
    }

    public List<UserPluginInfo> getStagingUserPluginInfo() {
        ArtifactoryServer artifactoryServer = getArtifactoryServer();
        return artifactoryServer.getStagingUserPluginInfo(this, null);
    }

    public PluginSettings getSelectedStagingPlugin() {
        return getDeployerDetails().getStagingPlugin();
    }

    private boolean isMultiConfProject(AbstractBuild build) {
        return (build.getProject().getClass().equals(MatrixConfiguration.class));
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractBuildWrapperDescriptor {
        private static final String DISPLAY_NAME = "Gradle-Artifactory Integration";
        private static final String CONFIG_PREFIX = "gradle";

        public DescriptorImpl() {
            super(ArtifactoryGradleConfigurator.class, DISPLAY_NAME, CONFIG_PREFIX);
        }

        protected DescriptorImpl(Class<? extends BuildWrapper> clazz) {
            super(clazz, DISPLAY_NAME, CONFIG_PREFIX);
        }

        @SuppressWarnings("unused")
        @JavaScriptMethod
        public RefreshServerResponse refreshFromArtifactory(String url, String credentialsId, String username, String password, boolean overrideCredentials) {
            return super.refreshDeployersFromArtifactory(url, credentialsId, username, password, overrideCredentials, true);
        }

        @JavaScriptMethod
        public RefreshServerResponse refreshResolversFromArtifactory(String url, String credentialsId, String username, String password, boolean overrideCredentials) {
            return super.refreshResolversFromArtifactory(url, credentialsId, username, password, overrideCredentials);
        }

        @SuppressWarnings("unused")
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            return PluginsUtils.fillPluginCredentials(project);
        }
    }

    /**
     * This run listener handles the job completed event to cleanup svn tags and working copy in case of build failure.
     */
    @Extension
    public static final class ReleaseRunListener extends RunListener<AbstractBuild> {
        /**
         * Completed event is sent after the build and publishers execution. The build result in this stage is final and
         * cannot be modified. So this is a good place to revert working copy and tag if the build failed.
         */
        @Override
        public void onCompleted(AbstractBuild run, TaskListener listener) {
            if (!(run instanceof FreeStyleBuild)) {
                return;
            }

            BaseGradleReleaseAction releaseAction = run.getAction(GradleReleaseAction.class);
            if (releaseAction == null) {
                releaseAction = run.getAction(GradleReleaseApiAction.class);
            }
            if (releaseAction == null) {
                return;
            }

            // signal completion to the scm coordinator
            ArtifactoryGradleConfigurator wrapper = ActionableHelper.getBuildWrapper(
                    (BuildableItemWithBuildWrappers) run.getProject(), ArtifactoryGradleConfigurator.class);

            Result result = run.getResult();
            boolean successRun = result.isBetterOrEqualTo(Result.SUCCESS);

            if (!wrapper.isAllowPromotionOfNonStagedBuilds()) {
                if (successRun) {
                    // add a stage action
                    run.addAction(new UnifiedPromoteBuildAction(run, wrapper));
                }
            }

            try {
                wrapper.getReleaseWrapper().getScmCoordinator().buildCompleted();
            } catch (Exception e) {
                run.setResult(Result.FAILURE);
                listener.error("[RELEASE] Failed on build completion");
                e.printStackTrace(listener.getLogger());
            }

            // once the build is completed reset the version maps. Since the GradleReleaseAction is saved
            // in memory and is only build when re-saving a project's config or during startup, therefore
            // a cleanup of the internal maps is needed.
            releaseAction.reset();

            // remove the release action from the build. the stage action is the point of interaction for successful builds
            run.getActions().remove(releaseAction);
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
