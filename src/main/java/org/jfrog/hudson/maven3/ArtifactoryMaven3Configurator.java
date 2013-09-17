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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.BuildInfoAwareConfigurator;
import org.jfrog.hudson.BuildInfoResultAction;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.release.UnifiedPromoteBuildAction;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.FormValidations;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.OverridingDeployerCredentialsConverter;
import org.jfrog.hudson.util.PublisherContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Freestyle Maven 3 configurator. Currently for publishing only.
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryMaven3Configurator extends BuildWrapper implements DeployerOverrider,
        BuildInfoAwareConfigurator {

    /**
     * Repository URL and repository to deploy artifacts to
     */
    private final ServerDetails details;
    private final Credentials overridingDeployerCredentials;
    /**
     * If checked (default) deploy maven artifacts
     */
    private final boolean deployArtifacts;
    private final IncludesExcludes artifactDeploymentPatterns;

    /**
     * Include environment variables in the generated build info
     */
    private final boolean includeEnvVars;

    private final boolean deployBuildInfo;
    private IncludesExcludes envVarsPatterns;
    private final boolean runChecks;

    private final String violationRecipients;

    private final boolean includePublishArtifacts;

    private final String scopes;

    private boolean licenseAutoDiscovery;
    private boolean disableLicenseAutoDiscovery;
    private final boolean discardOldBuilds;
    private final boolean discardBuildArtifacts;
    private final String matrixParams;
    private final boolean enableIssueTrackerIntegration;
    private boolean aggregateBuildIssues;
    private String aggregationBuildStatus;

    private boolean blackDuckRunChecks;
    private String blackDuckAppName;
    private String blackDuckAppVersion;
    private String blackDuckReportRecipients; //csv
    private String blackDuckScopes; //csv
    private boolean blackDuckIncludePublishedArtifacts;
    private boolean autoCreateMissingComponentRequests;
    private boolean autoDiscardStaleComponentRequests;

    @DataBoundConstructor
    public ArtifactoryMaven3Configurator(ServerDetails details, Credentials overridingDeployerCredentials,
            IncludesExcludes artifactDeploymentPatterns, boolean deployArtifacts, boolean deployBuildInfo,
            boolean includeEnvVars, IncludesExcludes envVarsPatterns,
            boolean runChecks, String violationRecipients, boolean includePublishArtifacts,
            String scopes, boolean disableLicenseAutoDiscovery, boolean discardOldBuilds,
            boolean discardBuildArtifacts, String matrixParams,
            boolean enableIssueTrackerIntegration, boolean aggregateBuildIssues, String aggregationBuildStatus,
            boolean blackDuckRunChecks, String blackDuckAppName, String blackDuckAppVersion,
            String blackDuckReportRecipients, String blackDuckScopes, boolean blackDuckIncludePublishedArtifacts,
            boolean autoCreateMissingComponentRequests, boolean autoDiscardStaleComponentRequests) {
        this.details = details;
        this.overridingDeployerCredentials = overridingDeployerCredentials;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.envVarsPatterns = envVarsPatterns;
        this.runChecks = runChecks;
        this.violationRecipients = violationRecipients;
        this.includePublishArtifacts = includePublishArtifacts;
        this.scopes = scopes;
        this.discardOldBuilds = discardOldBuilds;
        this.discardBuildArtifacts = discardBuildArtifacts;
        this.matrixParams = matrixParams;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.licenseAutoDiscovery = !disableLicenseAutoDiscovery;
        this.deployBuildInfo = deployBuildInfo;
        this.deployArtifacts = deployArtifacts;
        this.includeEnvVars = includeEnvVars;
        this.blackDuckRunChecks = blackDuckRunChecks;
        this.blackDuckAppName = blackDuckAppName;
        this.blackDuckAppVersion = blackDuckAppVersion;
        this.blackDuckReportRecipients = blackDuckReportRecipients;
        this.blackDuckScopes = blackDuckScopes;
        this.blackDuckIncludePublishedArtifacts = blackDuckIncludePublishedArtifacts;
        this.autoCreateMissingComponentRequests = autoCreateMissingComponentRequests;
        this.autoDiscardStaleComponentRequests = autoDiscardStaleComponentRequests;
    }

    // NOTE: The following getters are used by jelly. Do not remove them

    public ServerDetails getDetails() {
        return details;
    }

    public boolean isDiscardOldBuilds() {
        return discardOldBuilds;
    }

    public boolean isDiscardBuildArtifacts() {
        return discardBuildArtifacts;
    }

    public boolean isOverridingDefaultDeployer() {
        return (getOverridingDeployerCredentials() != null);
    }

    public Credentials getOverridingDeployerCredentials() {
        return overridingDeployerCredentials;
    }

    public boolean isDeployArtifacts() {
        return deployArtifacts;
    }

    public String getMatrixParams() {
        return matrixParams;
    }

    public IncludesExcludes getArtifactDeploymentPatterns() {
        return artifactDeploymentPatterns;
    }

    public boolean isDeployBuildInfo() {
        return deployBuildInfo;
    }

    public ArtifactoryServer getArtifactoryServer() {
        List<ArtifactoryServer> servers = getDescriptor().getArtifactoryServers();
        for (ArtifactoryServer server : servers) {
            if (server.getName().equals(getArtifactoryName())) {
                return server;
            }
        }
        return null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getRepositoryKey() {
        return details != null ? details.repositoryKey : null;
    }

    public boolean isIncludePublishArtifacts() {
        return includePublishArtifacts;
    }

    /**
     * @return The snapshots deployment repository. If not defined the releases deployment repository will be returned
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public String getSnapshotsRepositoryKey() {
        return details != null ?
                (details.snapshotsRepositoryKey != null ? details.snapshotsRepositoryKey : details.repositoryKey) :
                null;
    }

    public boolean isLicenseAutoDiscovery() {
        return licenseAutoDiscovery;
    }

    public boolean isDisableLicenseAutoDiscovery() {
        return disableLicenseAutoDiscovery;
    }

    public String getArtifactoryName() {
        return details != null ? details.artifactoryName : null;
    }

    public String getArtifactoryUrl() {
        return details != null ? details.getArtifactoryUrl() : null;
    }

    public String getScopes() {
        return scopes;
    }

    public boolean isIncludeEnvVars() {
        return includeEnvVars;
    }

    public IncludesExcludes getEnvVarsPatterns() {
        return envVarsPatterns;
    }

    public String getViolationRecipients() {
        return violationRecipients;
    }

    public boolean isRunChecks() {
        return runChecks;
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

    public boolean isEnableIssueTrackerIntegration() {
        return enableIssueTrackerIntegration;
    }

    public boolean isAggregateBuildIssues() {
        return aggregateBuildIssues;
    }

    public String getAggregationBuildStatus() {
        return aggregationBuildStatus;
    }

    public ArtifactoryServer getArtifactoryServer(String artifactoryServerName) {
        List<ArtifactoryServer> servers = getDescriptor().getArtifactoryServers();
        for (ArtifactoryServer server : servers) {
            if (server.getName().equals(artifactoryServerName)) {
                return server;
            }
        }
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject project) {
        return ActionableHelper.getArtifactoryProjectAction(details.getArtifactoryUrl(), project);
    }

    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException {

        final String artifactoryServerName = getArtifactoryName();
        if (StringUtils.isBlank(artifactoryServerName)) {
            return super.setUp(build, launcher, listener);
        }
        final ArtifactoryServer artifactoryServer = getArtifactoryServer(artifactoryServerName);
        if (artifactoryServer == null) {
            listener.getLogger().format("No Artifactory server configured for %s. " +
                    "Please check your configuration.", artifactoryServerName).println();
            build.setResult(Result.FAILURE);
            throw new IllegalArgumentException("No Artifactory server configured for " + artifactoryServerName);
        }
        final PublisherContext context = new PublisherContext.Builder().artifactoryServer(artifactoryServer)
                .serverDetails(getDetails()).deployerOverrider(ArtifactoryMaven3Configurator.this)
                .runChecks(isRunChecks()).includePublishArtifacts(isIncludePublishArtifacts())
                .violationRecipients(getViolationRecipients()).scopes(getScopes())
                .licenseAutoDiscovery(isLicenseAutoDiscovery()).discardOldBuilds(isDiscardOldBuilds())
                .deployArtifacts(isDeployArtifacts()).includesExcludes(getArtifactDeploymentPatterns())
                .skipBuildInfoDeploy(skipBuildInfoDeploy)
                .includeEnvVars(isIncludeEnvVars()).envVarsPatterns(getEnvVarsPatterns())
                .discardBuildArtifacts(isDiscardBuildArtifacts()).matrixParams(getMatrixParams())
                .enableIssueTrackerIntegration(isEnableIssueTrackerIntegration())
                .aggregateBuildIssues(isAggregateBuildIssues()).aggregationBuildStatus(getAggregationBuildStatus())
                .integrateBlackDuck(isBlackDuckRunChecks(), getBlackDuckAppName(), getBlackDuckAppVersion(),
                        getBlackDuckReportRecipients(), getBlackDuckScopes(), isBlackDuckIncludePublishedArtifacts(),
                        isAutoCreateMissingComponentRequests(), isAutoDiscardStaleComponentRequests())
                .build();
        build.setResult(Result.SUCCESS);
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                try {
                    ExtractorUtils.addBuilderInfoArguments(env, build, listener, context, null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) {
                Result result = build.getResult();
                if (deployBuildInfo && result != null && result.isBetterOrEqualTo(Result.SUCCESS)) {
                    build.getActions().add(new BuildInfoResultAction(getArtifactoryUrl(), build));
                    build.getActions().add(new UnifiedPromoteBuildAction<ArtifactoryMaven3Configurator>(build,
                            ArtifactoryMaven3Configurator.this));
                }
                return true;
            }
        };
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        public DescriptorImpl() {
            super(ArtifactoryMaven3Configurator.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return item.getClass().isAssignableFrom(FreeStyleProject.class);
        }

        @Override
        public String getDisplayName() {
            return "Maven3-Artifactory Integration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindParameters(this, "maven3");
            save();
            return true;
        }

        public FormValidation doCheckViolationRecipients(@QueryParameter String value) {
            return FormValidations.validateEmails(value);
        }

        /**
         * Returns the list of {@link org.jfrog.hudson.ArtifactoryServer} configured.
         *
         * @return can be empty but never null.
         */
        public List<ArtifactoryServer> getArtifactoryServers() {
            ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl)
                    Hudson.getInstance().getDescriptor(ArtifactoryBuilder.class);
            return descriptor.getArtifactoryServers();
        }

        public boolean isJiraPluginEnabled() {
            return (Jenkins.getInstance().getPlugin("jira") != null);
        }
    }

    /**
     * Convert any remaining local credential variables to a credentials object
     */
    public static final class ConverterImpl extends OverridingDeployerCredentialsConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }
    }

    /**
     * @deprecated: Use org.jfrog.hudson.DeployerOverrider#getOverridingDeployerCredentials()
     */
    @Deprecated
    private transient String username;

    /**
     * @deprecated: Use org.jfrog.hudson.DeployerOverrider#getOverridingDeployerCredentials()
     */
    @Deprecated
    private transient String scrambledPassword;

    /**
     * @deprecated: Use org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator#deployBuildInfo
     */
    @Deprecated
    private transient boolean skipBuildInfoDeploy;

}
