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

package org.jfrog.hudson.maven3;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixConfiguration;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.ListBoxModel;
import hudson.util.XStream2;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.*;
import org.jfrog.hudson.action.ActionableHelper;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Freestyle Maven 3 configurator.
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryMaven3Configurator extends BuildWrapper implements DeployerOverrider, ResolverOverrider,
        BuildInfoAwareConfigurator, MultiConfigurationAware {

    /**
     * Repository URL and repository to deploy artifacts to
     */
    private final ServerDetails deployerDetails;
    private final ServerDetails resolverDetails;
    private CredentialsConfig deployerCredentialsConfig;
    private CredentialsConfig resolverCredentialsConfig;

    /**
     * If checked (default) deploy maven artifacts
     */
    private boolean deployArtifacts;
    private IncludesExcludes artifactDeploymentPatterns;

    /**
     * Include environment variables in the generated build info
     */
    private boolean includeEnvVars;

    private boolean deployBuildInfo;
    private boolean discardOldBuilds;
    private boolean discardBuildArtifacts;
    private boolean asyncBuildRetention;
    private String deploymentProperties;
    private boolean enableIssueTrackerIntegration;
    private boolean filterExcludedArtifactsFromBuild;
    private boolean enableResolveArtifacts;
    private IncludesExcludes envVarsPatterns;
    private boolean aggregateBuildIssues;
    private String aggregationBuildStatus;
    private boolean recordAllDependencies;
    private String artifactoryCombinationFilter;
    private String customBuildName;
    private boolean overrideBuildName;
    /**
     * @deprecated: Use org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator#getDeployerCredentialsConfig()()
     */
    @Deprecated
    private Credentials overridingDeployerCredentials;
    /**
     * @deprecated: Use org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator#getResolverCredentialsId()()
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

    @DataBoundConstructor
    public ArtifactoryMaven3Configurator(ServerDetails details, ServerDetails deployerDetails, ServerDetails resolverDetails,
                                         CredentialsConfig deployerCredentialsConfig, CredentialsConfig resolverCredentialsConfig,
                                         boolean enableResolveArtifacts, IncludesExcludes artifactDeploymentPatterns,
                                         boolean deployArtifacts, boolean deployBuildInfo, boolean includeEnvVars,
                                         IncludesExcludes envVarsPatterns,
                                         boolean discardOldBuilds, boolean discardBuildArtifacts,
                                         boolean asyncBuildRetention, String matrixParams, String deploymentProperties,
                                         boolean enableIssueTrackerIntegration, boolean aggregateBuildIssues,
                                         String aggregationBuildStatus, boolean recordAllDependencies,
                                         boolean filterExcludedArtifactsFromBuild,
                                         String customBuildName,
                                         boolean overrideBuildName,
                                         String artifactoryCombinationFilter
    ) {
        this.deployerDetails = deployerDetails;
        this.resolverDetails = resolverDetails;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.resolverCredentialsConfig = resolverCredentialsConfig;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.envVarsPatterns = envVarsPatterns;
        this.discardOldBuilds = discardOldBuilds;
        this.discardBuildArtifacts = discardBuildArtifacts;
        this.asyncBuildRetention = asyncBuildRetention;
        this.deploymentProperties = deploymentProperties;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.recordAllDependencies = recordAllDependencies;
        this.filterExcludedArtifactsFromBuild = filterExcludedArtifactsFromBuild;
        this.deployBuildInfo = deployBuildInfo;
        this.deployArtifacts = deployArtifacts;
        this.includeEnvVars = includeEnvVars;
        this.artifactoryCombinationFilter = artifactoryCombinationFilter;
        this.enableResolveArtifacts = enableResolveArtifacts;
        this.customBuildName = customBuildName;
        this.overrideBuildName = overrideBuildName;
    }

    /**
     * Constructor for the DeployerResolverOverriderConverterTest
     *
     * @param details         - Old server details
     * @param deployerDetails - New deployer details
     * @param resolverDetails - new resolver details
     */
    public ArtifactoryMaven3Configurator(ServerDetails details, ServerDetails deployerDetails, ServerDetails resolverDetails) {
        this.details = details;
        this.deployerDetails = deployerDetails;
        this.resolverDetails = resolverDetails;
    }

    public ServerDetails getDeployerDetails() {
        return deployerDetails;
    }

    public ServerDetails getResolverDetails() {
        return resolverDetails;
    }

    public String getDownloadReleaseRepositoryKey() {
        return resolverDetails != null ? resolverDetails.getResolveReleaseRepositoryKey() : null;
    }

    public String getDownloadSnapshotRepositoryKey() {
        return resolverDetails != null ? resolverDetails.getResolveReleaseRepositoryKey() : null;
    }

    public boolean isDiscardOldBuilds() {
        return discardOldBuilds;
    }

    public boolean isDiscardBuildArtifacts() {
        return discardBuildArtifacts;
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

    public boolean isOverridingDefaultResolver() {
        return resolverCredentialsConfig.isCredentialsProvided();
    }

    public Credentials getOverridingResolverCredentials() {
        return overridingResolverCredentials;
    }

    public CredentialsConfig getResolverCredentialsConfig() {
        return resolverCredentialsConfig;
    }

    public boolean isOverridingResolverCredentials() {
        return resolverCredentialsConfig.isCredentialsProvided();
    }

    public boolean isDeployArtifacts() {
        return deployArtifacts;
    }

    public String getDeploymentProperties() {
        return deploymentProperties;
    }

    public IncludesExcludes getArtifactDeploymentPatterns() {
        return artifactDeploymentPatterns;
    }

    public boolean isDeployBuildInfo() {
        return deployBuildInfo;
    }

    public String getCustomBuildName() {
        return customBuildName;
    }

    public boolean isOverrideBuildName() {
        return overrideBuildName;
    }

    public boolean isRecordAllDependencies() {
        return recordAllDependencies;
    }

    public ArtifactoryServer getArtifactoryServer() {
        return RepositoriesUtils.getArtifactoryServer(getArtifactoryName(), getDescriptor().getArtifactoryServers());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getRepositoryKey() {
        return getDeployerDetails() != null ? getDeployerDetails().getDeployReleaseRepositoryKey() : null;
    }

    public String getDefaultPromotionTargetRepository() {
        //Not implemented
        return null;
    }

    /**
     * @return The snapshots deployment repository. If not defined the releases deployment repository will be returned
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public String getSnapshotsRepositoryKey() {
        return getDeployerDetails() != null ?
                (getDeployerDetails().getDeploySnapshotRepository() != null ? getDeployerDetails().getDeploySnapshotRepository().getRepoKey() :
                        getDeployerDetails().getDeployReleaseRepository().getRepoKey()) :
                null;
    }

    public String getArtifactoryName() {
        return getDeployerDetails() != null ? getDeployerDetails().artifactoryName : null;
    }

    public String getArtifactoryUrl() {
        ArtifactoryServer server = getArtifactoryServer();
        return server != null ? server.getArtifactoryUrl() : null;
    }

    public boolean isIncludeEnvVars() {
        return includeEnvVars;
    }

    public IncludesExcludes getEnvVarsPatterns() {
        return envVarsPatterns;
    }

    public boolean isEnableIssueTrackerIntegration() {
        return enableIssueTrackerIntegration;
    }

    public boolean isAggregateBuildIssues() {
        return aggregateBuildIssues;
    }

    public boolean isEnableResolveArtifacts() {
        return enableResolveArtifacts;
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

    public ArtifactoryServer getArtifactoryServer(String artifactoryServerName) {
        return RepositoriesUtils.getArtifactoryServer(artifactoryServerName, getDescriptor().getArtifactoryServers());
    }

    public List<Repository> getReleaseRepositoryList() {
        return RepositoriesUtils.collectRepositories(getDeployerDetails().getDeployReleaseRepository().getKeyFromSelect());
    }

    public List<Repository> getSnapshotRepositoryList() {
        return RepositoriesUtils.collectRepositories(getDeployerDetails().getDeploySnapshotRepository().getKeyFromSelect());
    }

    public List<VirtualRepository> getResolveReleaseRepositoryList() {
        return RepositoriesUtils.collectVirtualRepositories(null, resolverDetails.getResolveReleaseRepository().getKeyFromSelect());
    }

    public List<VirtualRepository> getResolveSnapshotRepositoryList() {
        return RepositoriesUtils.collectVirtualRepositories(null, resolverDetails.getResolveSnapshotRepository().getKeyFromSelect());
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

        final String artifactoryServerName = getArtifactoryName();
        if (StringUtils.isBlank(artifactoryServerName)) {
            return super.setUp(build, launcher, listener);
        }
        final ArtifactoryServer artifactoryServer = getArtifactoryServer(artifactoryServerName);
        if (artifactoryServer == null) {
            listener.getLogger().format("No Artifactory server configured for %s. " +
                    "Please check your configuration.", artifactoryServerName).println();
            throw new IllegalArgumentException("No Artifactory server configured for " + artifactoryServerName);
        }

        PublisherContext.Builder publisherBuilder = new PublisherContext.Builder().artifactoryServer(artifactoryServer)
                .serverDetails(getDeployerDetails()).deployerOverrider(ArtifactoryMaven3Configurator.this)
                .discardOldBuilds(isDiscardOldBuilds())
                .deployArtifacts(isDeployArtifacts()).includesExcludes(getArtifactDeploymentPatterns())
                .skipBuildInfoDeploy(!deployBuildInfo).recordAllDependencies(isRecordAllDependencies())
                .includeEnvVars(isIncludeEnvVars()).envVarsPatterns(getEnvVarsPatterns())
                .discardBuildArtifacts(isDiscardBuildArtifacts()).asyncBuildRetention(isAsyncBuildRetention())
                .deploymentProperties(getDeploymentProperties()).enableIssueTrackerIntegration(isEnableIssueTrackerIntegration())
                .aggregateBuildIssues(isAggregateBuildIssues()).aggregationBuildStatus(getAggregationBuildStatus())
                .filterExcludedArtifactsFromBuild(isFilterExcludedArtifactsFromBuild())
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion())
                .overrideBuildName(isOverrideBuildName())
                .customBuildName(getCustomBuildName());

        if (isMultiConfProject(build) && isDeployArtifacts()) {
            if (StringUtils.isBlank(getArtifactoryCombinationFilter())) {
                String error = "The field \"Combination Matches\" is empty, but is defined as mandatory!";
                listener.getLogger().println(error);
                throw new IllegalArgumentException(error);
            }
            boolean isFiltered = MultiConfigurationUtils.isfiltrated(build, getArtifactoryCombinationFilter());
            if (isFiltered) {
                publisherBuilder.skipBuildInfoDeploy(true).deployArtifacts(false);
            }
        }

        ResolverContext resolver = null;
        if (isEnableResolveArtifacts()) {
            CredentialsConfig credentialResolver = CredentialManager.getPreferredResolver(
                    ArtifactoryMaven3Configurator.this, getArtifactoryServer());
            resolver = new ResolverContext(getArtifactoryServer(), getResolverDetails(), credentialResolver.provideCredentials(build.getProject()),
                    ArtifactoryMaven3Configurator.this);
        }
        final ResolverContext resolverContext = resolver;
        final PublisherContext publisherContext = publisherBuilder.build();
        build.setResult(Result.SUCCESS);

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                try {
                    ExtractorUtils.addBuilderInfoArguments(env, build, listener, publisherContext, resolverContext, build.getWorkspace(), launcher);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) {
                Result result = build.getResult();
                if (deployBuildInfo && result != null && result.isBetterOrEqualTo(Result.SUCCESS)) {
                    String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(ArtifactoryMaven3Configurator.this, build);
                    build.addAction(new BuildInfoResultAction(getArtifactoryUrl(), build, buildName));
                    build.addAction(new UnifiedPromoteBuildAction(build, ArtifactoryMaven3Configurator.this));
                }
                return true;
            }
        };
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
        private static final String DISPLAY_NAME = "Maven3-Artifactory Integration";
        private static final String CONFIG_PREFIX = "maven3";

        public DescriptorImpl() {
            super(ArtifactoryMaven3Configurator.class, DISPLAY_NAME, CONFIG_PREFIX);
            load();
        }

        @SuppressWarnings("unused")
        @JavaScriptMethod
        public RefreshServerResponse refreshFromArtifactory(String url, String credentialsId, String username, String password, boolean overrideCredentials) {
            return super.refreshDeployersFromArtifactory(url, credentialsId, username, password, overrideCredentials, false);
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
     * Page Converter
     */
    public static final class ConverterImpl extends DeployerResolverOverriderConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }
    }
}
