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

package org.jfrog.hudson.ivy;

import com.google.common.collect.Iterables;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.remoting.Which;
import hudson.tasks.Ant;
import hudson.tasks.BuildWrapper;
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
import org.jfrog.hudson.release.promotion.UnifiedPromoteBuildAction;
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
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jfrog.hudson.util.plugins.MultiConfigurationUtils.isfiltrated;
import static org.jfrog.hudson.util.plugins.MultiConfigurationUtils.validateCombinationFilter;


/**
 * Gradle-Artifactory plugin configuration, allows to add the server details, deployment username/password, as well as
 * flags to deploy ivy, maven, and artifacts, as well as specifications of the location of the remote plugin (.gradle)
 * groovy script.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryIvyFreeStyleConfigurator extends BuildWrapper implements DeployerOverrider,
        BuildInfoAwareConfigurator, MultiConfigurationAware {

    public final String remotePluginLocation;
    public final boolean deployBuildInfo;
    public final boolean includeEnvVars;
    private final CredentialsConfig deployerCredentialsConfig;
    private final String ivyPattern;
    private final String artifactPattern;
    private final Boolean useMavenPatterns;
    private final IncludesExcludes artifactDeploymentPatterns;
    private final boolean discardOldBuilds;
    private final boolean asyncBuildRetention;
    private final boolean passIdentifiedDownstream;
    private final boolean discardBuildArtifacts;
    private final String deploymentProperties;
    private final boolean enableIssueTrackerIntegration;
    private final boolean filterExcludedArtifactsFromBuild;
    private ServerDetails deployerDetails;
    private boolean deployArtifacts;
    private IncludesExcludes envVarsPatterns;
    private String aggregationBuildStatus;
    private boolean aggregateBuildIssues;
    private String artifactoryCombinationFilter;
    private String customBuildName;
    private boolean overrideBuildName;

    @Deprecated
    private Credentials overridingDeployerCredentials;

    /**
     * @deprecated: The following deprecated variables have corresponding converters to the variables replacing them
     */
    @Deprecated
    private final ServerDetails details = null;
    @Deprecated
    private final String matrixParams = null;
    @Deprecated
    private final Boolean notM2Compatible = null;

    @DataBoundConstructor
    public ArtifactoryIvyFreeStyleConfigurator(ServerDetails details, ServerDetails deployerDetails, CredentialsConfig deployerCredentialsConfig,
                                               boolean deployArtifacts, String remotePluginLocation,
                                               boolean includeEnvVars, IncludesExcludes envVarsPatterns,
                                               boolean deployBuildInfo, String ivyPattern,
                                               String artifactPattern, Boolean useMavenPatterns, Boolean notM2Compatible, IncludesExcludes artifactDeploymentPatterns,
                                               boolean discardOldBuilds, boolean asyncBuildRetention, boolean passIdentifiedDownstream, boolean discardBuildArtifacts,
                                               String matrixParams, String deploymentProperties, boolean enableIssueTrackerIntegration, boolean aggregateBuildIssues,
                                               String aggregationBuildStatus, boolean filterExcludedArtifactsFromBuild,
                                               String artifactoryCombinationFilter, String customBuildName, boolean overrideBuildName) {
        this.deployerDetails = deployerDetails;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.deployArtifacts = deployArtifacts;
        this.remotePluginLocation = remotePluginLocation;
        this.includeEnvVars = includeEnvVars;
        this.envVarsPatterns = envVarsPatterns;
        this.deployBuildInfo = deployBuildInfo;
        this.ivyPattern = ivyPattern;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.filterExcludedArtifactsFromBuild = filterExcludedArtifactsFromBuild;
        this.artifactPattern = clearApostrophes(artifactPattern);
        this.useMavenPatterns = useMavenPatterns;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.discardOldBuilds = discardOldBuilds;
        this.asyncBuildRetention = asyncBuildRetention;
        this.passIdentifiedDownstream = passIdentifiedDownstream;
        this.deploymentProperties = deploymentProperties;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.discardBuildArtifacts = discardBuildArtifacts;
        this.artifactoryCombinationFilter = artifactoryCombinationFilter;
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
        return clearApostrophes(artifactPattern);
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

    public String getDefaultPromotionTargetRepository() {
        //Not implemented
        return null;
    }

    public String getArtifactoryName() {
        return getDeployerDetails() != null ? getDeployerDetails().artifactoryName : null;
    }

    public String getArtifactoryUrl() {
        ArtifactoryServer server = getArtifactoryServer();
        return server != null ? server.getUrl() : null;
    }

    public boolean isDeployArtifacts() {
        return deployArtifacts;
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
        listener.getLogger().println(
                "Jenkins Artifactory Plugin version: " + ActionableHelper.getArtifactoryPluginVersion());
        PublisherContext.Builder publisherBuilder = getBuilder();
        RepositoriesUtils.validateServerConfig(build, listener, getArtifactoryServer(), getArtifactoryUrl());
        int totalBuilds = 1;

        if (isMultiConfProject(build)) {
            totalBuilds = ((MatrixProject) build.getParent().getParent()).getActiveConfigurations().size();
            if (isDeployArtifacts()) {
                validateCombinationFilter(build, listener, getArtifactoryCombinationFilter());
                boolean isFiltered = isfiltrated(build, getArtifactoryCombinationFilter());
                if (isFiltered) {
                    publisherBuilder.skipBuildInfoDeploy(true).deployArtifacts(false);
                }
            }
        }

        final Ant antBuild = getLastAntBuild(build.getProject());

        if (antBuild != null) {
            // The ConcurrentBuildSetupSync helper class is used to make sure that the code
            // inside its setUp() method is invoked by only one job in this build
            // (matrix project builds include more that one job) and that all other jobs
            // wait till the seUup() method finishes.
            new ConcurrentJobsHelper.ConcurrentBuildSetupSync(build, totalBuilds) {
                @Override
                public void setUp() {
                    // Obtain the current build and use it to store the configured targets.
                    // We store them because we override them during the build and we'll need
                    // their original values at the tear down stage so that they can be restored.
                    ConcurrentJobsHelper.ConcurrentBuild concurrentBuild = ConcurrentJobsHelper.getConcurrentBuild(build);
                    // Remove the Artifactory Plugin additional arguments, in case they are included in the targets string:
                    String targets = antBuild.getTargets() != null ? antBuild.getTargets().replace(getAntArgs(), "") : "";
                    concurrentBuild.putParam("targets", targets);
                    // Override the targets after we stored them:
                    setTargetsField(antBuild, targets + " " + getAntArgs());
                }
            };
        }

        build.setResult(Result.SUCCESS);
        final PublisherContext finalPublisherContext = publisherBuilder.build();

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                super.buildEnvVars(env);
                try {
                    String actualDependencyDirPath = actualDependencyDirPath(build, launcher);
                    env.put("ARTIFACTORY_CACHE_LIBS", actualDependencyDirPath);
                    ExtractorUtils.addBuilderInfoArguments(env, build, listener, finalPublisherContext, null, build.getWorkspace(), launcher);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean tearDown(final AbstractBuild build, BuildListener listener)
                    throws IOException, InterruptedException {
                Result result = build.getResult();

                if (antBuild != null) {
                    // The ConcurrentBuildTearDownSync helper class is used to make sure that the code
                    // inside its tearDown() method is invoked by only one job in this build
                    // (matrix project builds include more that one job) and that this
                    // job is the last one running.
                    new ConcurrentJobsHelper.ConcurrentBuildTearDownSync(build, result) {
                        @Override
                        public void tearDown() {
                            // Restore the original targets of this build (we overrided their
                            // values in the setUp stage):
                            ConcurrentJobsHelper.ConcurrentBuild concurrentBuild = ConcurrentJobsHelper.getConcurrentBuild(build);
                            String targets = concurrentBuild.getParam("targets");
                            // Remove the Artifactory Plugin additional arguments, in case they are included in the targets string:
                            targets = targets.replace(getAntArgs(), "");
                            setTargetsField(antBuild, targets);
                        }
                    };
                }

                if (!finalPublisherContext.isSkipBuildInfoDeploy() && (result == null ||
                        result.isBetterOrEqualTo(Result.SUCCESS))) {
                    String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(ArtifactoryIvyFreeStyleConfigurator.this, build);
                    build.getActions().add(0, new BuildInfoResultAction(getArtifactoryUrl(), build, buildName));
                    build.getActions().add(new UnifiedPromoteBuildAction(build, ArtifactoryIvyFreeStyleConfigurator.this));
                }

                // Aborted action by the user:
                if (Result.ABORTED.equals(result)) {
                    ConcurrentJobsHelper.removeConcurrentBuildJob(build);
                }
                return true;
            }
        };
    }

    private String actualDependencyDirPath(AbstractBuild build, Launcher launcher) throws IOException, InterruptedException {
        File localDependencyFile = Which.jarFile(ArtifactoryBuildListener.class);
        FilePath actualDependencyDir =
                PluginDependencyHelper.getActualDependencyDirectory(localDependencyFile, ActionableHelper.getNode(launcher).getRootPath());
        String actualDependencyDirPath = actualDependencyDir.getRemote();
        actualDependencyDirPath = actualDependencyDirPath.replace('\\', '/');
        actualDependencyDirPath = "\"" + actualDependencyDirPath + "\"";
        return actualDependencyDirPath;
    }

    private void setTargetsField(Ant builder, String targets) {
        try {
            Field targetsField = builder.getClass().getDeclaredField("targets");
            targetsField.setAccessible(true);
            targetsField.set(builder, targets);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getAntArgs() {
        String lib = "-lib ${ARTIFACTORY_CACHE_LIBS} ";
        String listener = "-listener org.jfrog.build.extractor.listener.ArtifactoryBuildListener";

        return lib + listener;
    }

    private Ant getLastAntBuild(AbstractProject project) {
        if (project instanceof Project) {
            List<Ant> ants = ActionableHelper.getBuilder((Project) project, Ant.class);
            return Iterables.getLast(ants, null);
        }
        return null;
    }

    private PublisherContext.Builder getBuilder() {
        return new PublisherContext.Builder().
                artifactoryServer(getArtifactoryServer())
                .serverDetails(getDeployerDetails()).deployerOverrider(ArtifactoryIvyFreeStyleConfigurator.this)
                .discardOldBuilds(isDiscardOldBuilds())
                .deployArtifacts(isDeployArtifacts()).includesExcludes(getArtifactDeploymentPatterns())
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .includeEnvVars(isIncludeEnvVars()).envVarsPatterns(getEnvVarsPatterns())
                .discardBuildArtifacts(isDiscardBuildArtifacts()).asyncBuildRetention(isAsyncBuildRetention())
                .deploymentProperties(getDeploymentProperties()).maven2Compatible(isUseMavenPatterns()).artifactsPattern(getArtifactPattern())
                .ivyPattern(getIvyPattern()).enableIssueTrackerIntegration(isEnableIssueTrackerIntegration())
                .aggregateBuildIssues(isAggregateBuildIssues()).aggregationBuildStatus(getAggregationBuildStatus())
                .filterExcludedArtifactsFromBuild(isFilterExcludedArtifactsFromBuild())
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion())
                .overrideBuildName(isOverrideBuildName())
                .customBuildName(getCustomBuildName());
    }

    public ArtifactoryServer getArtifactoryServer() {
        return RepositoriesUtils.getArtifactoryServer(getArtifactoryName(), getDescriptor().getArtifactoryServers());
    }

    public List<Repository> getReleaseRepositoryList() {
        return RepositoriesUtils.collectRepositories(getDeployerDetails().getDeploySnapshotRepository().getKeyFromSelect());
    }

    private boolean isMultiConfProject(AbstractBuild build) {
        return (build.getProject().getClass().equals(MatrixConfiguration.class));
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        private AbstractProject<?, ?> item;

        public DescriptorImpl() {
            super(ArtifactoryIvyFreeStyleConfigurator.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            this.item = item;
            return item.getClass().isAssignableFrom(FreeStyleProject.class) ||
                    item.getClass().isAssignableFrom(MatrixProject.class) ||
                    (Jenkins.getInstance().getPlugin(PluginsUtils.MULTIJOB_PLUGIN_ID) != null &&
                            item.getClass().isAssignableFrom(MultiJobProject.class));
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
            RefreshServerResponse response = new RefreshServerResponse();
            CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId, overrideCredentials);
            ArtifactoryServer artifactoryServer = RepositoriesUtils.getArtifactoryServer(url,
                    RepositoriesUtils.getArtifactoryServers());

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

            /*
            * In case of Exception, we write error in the Javascript scope!
            * */
            return response;
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            return PluginsUtils.fillPluginCredentials(project);
        }

        @Override
        public String getDisplayName() {
            return "Ant/Ivy-Artifactory Integration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindParameters(this, "ivy");
            save();
            return true;
        }

        public boolean isMultiConfProject() {
            return (item.getClass().isAssignableFrom(MatrixProject.class));
        }

        public FormValidation doCheckArtifactoryCombinationFilter(@QueryParameter String value)
                throws IOException, InterruptedException {
            return FormValidations.validateArtifactoryCombinationFilter(value);
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
