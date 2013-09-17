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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.remoting.Which;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.listener.ArtifactoryBuildListener;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.BuildInfoAwareConfigurator;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.FormValidations;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.OverridingDeployerCredentialsConverter;
import org.jfrog.hudson.util.PluginDependencyHelper;
import org.jfrog.hudson.util.PublisherContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * @author Tomer Cohen
 */
public class ArtifactoryIvyConfigurator extends AntIvyBuildWrapper implements DeployerOverrider,
        BuildInfoAwareConfigurator {

    private ServerDetails details;
    private final Credentials overridingDeployerCredentials;
    private boolean deployArtifacts;
    private final IncludesExcludes artifactDeploymentPatterns;
    private boolean deployBuildInfo;
    private boolean includeEnvVars;
    private IncludesExcludes envVarsPatterns;
    private boolean runChecks;
    private String violationRecipients;
    private boolean includePublishArtifacts;
    private String scopes;
    private boolean licenseAutoDiscovery;
    private boolean disableLicenseAutoDiscovery;
    private boolean discardOldBuilds;
    private final boolean discardBuildArtifacts;
    private final String matrixParams;
    private boolean notM2Compatible;
    private String ivyPattern;
    private String aggregationBuildStatus;
    private String artifactPattern;
    private boolean enableIssueTrackerIntegration;
    private boolean aggregateBuildIssues;
    private boolean blackDuckRunChecks;
    private String blackDuckAppName;
    private String blackDuckAppVersion;
    private String blackDuckReportRecipients; //csv
    private String blackDuckScopes; //csv
    private boolean blackDuckIncludePublishedArtifacts;
    private boolean autoCreateMissingComponentRequests;
    private boolean autoDiscardStaleComponentRequests;

    @DataBoundConstructor
    public ArtifactoryIvyConfigurator(ServerDetails details, Credentials overridingDeployerCredentials,
            boolean deployArtifacts, IncludesExcludes artifactDeploymentPatterns, boolean deployBuildInfo,
            boolean includeEnvVars, IncludesExcludes envVarsPatterns,
            boolean runChecks, String violationRecipients, boolean includePublishArtifacts,
            String scopes, boolean disableLicenseAutoDiscovery, boolean notM2Compatible, String ivyPattern,
            String artifactPattern, boolean discardOldBuilds, boolean discardBuildArtifacts, String matrixParams,
            boolean enableIssueTrackerIntegration, boolean aggregateBuildIssues, String aggregationBuildStatus,
            boolean blackDuckRunChecks, String blackDuckAppName, String blackDuckAppVersion,
            String blackDuckReportRecipients, String blackDuckScopes, boolean blackDuckIncludePublishedArtifacts,
            boolean autoCreateMissingComponentRequests, boolean autoDiscardStaleComponentRequests) {
        this.details = details;
        this.overridingDeployerCredentials = overridingDeployerCredentials;
        this.deployArtifacts = deployArtifacts;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.deployBuildInfo = deployBuildInfo;
        this.includeEnvVars = includeEnvVars;
        this.envVarsPatterns = envVarsPatterns;
        this.runChecks = runChecks;
        this.violationRecipients = violationRecipients;
        this.includePublishArtifacts = includePublishArtifacts;
        this.scopes = scopes;
        this.disableLicenseAutoDiscovery = disableLicenseAutoDiscovery;
        this.notM2Compatible = notM2Compatible;
        this.ivyPattern = ivyPattern;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.artifactPattern = clearApostrophes(artifactPattern);
        this.discardOldBuilds = discardOldBuilds;
        this.discardBuildArtifacts = discardBuildArtifacts;
        this.matrixParams = matrixParams;
        this.licenseAutoDiscovery = !disableLicenseAutoDiscovery;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.blackDuckRunChecks = blackDuckRunChecks;
        this.blackDuckAppName = blackDuckAppName;
        this.blackDuckAppVersion = blackDuckAppVersion;
        this.blackDuckReportRecipients = blackDuckReportRecipients;
        this.blackDuckScopes = blackDuckScopes;
        this.blackDuckIncludePublishedArtifacts = blackDuckIncludePublishedArtifacts;
        this.autoCreateMissingComponentRequests = autoCreateMissingComponentRequests;
        this.autoDiscardStaleComponentRequests = autoDiscardStaleComponentRequests;
    }

    /**
     * Clears the extra apostrophes from the start and the end of the string
     */
    private String clearApostrophes(String artifactPattern) {
        return StringUtils.removeEnd(StringUtils.removeStart(artifactPattern, "\""), "\"");
    }

    public ServerDetails getDetails() {
        return details;
    }

    public String getMatrixParams() {
        return matrixParams;
    }

    public boolean isOverridingDefaultDeployer() {
        return (getOverridingDeployerCredentials() != null);
    }

    public Credentials getOverridingDeployerCredentials() {
        return overridingDeployerCredentials;
    }

    public boolean isNotM2Compatible() {
        return notM2Compatible;
    }

    public boolean isDiscardBuildArtifacts() {
        return discardBuildArtifacts;
    }

    public void setNotM2Compatible(boolean notM2Compatible) {
        this.notM2Compatible = notM2Compatible;
    }

    public boolean isDiscardOldBuilds() {
        return discardOldBuilds;
    }

    public void setDiscardOldBuilds(boolean discardOldBuilds) {
        this.discardOldBuilds = discardOldBuilds;
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

    public boolean isM2Compatible() {
        return !notM2Compatible;
    }

    public boolean isIncludePublishArtifacts() {
        return includePublishArtifacts;
    }

    public void setIncludePublishArtifacts(boolean includePublishArtifacts) {
        this.includePublishArtifacts = includePublishArtifacts;
    }

    public boolean isRunChecks() {
        return runChecks;
    }

    public boolean isDisableLicenseAutoDiscovery() {
        return disableLicenseAutoDiscovery;
    }

    public void setLicenseAutoDiscovery(boolean licenseAutoDiscovery) {
        this.licenseAutoDiscovery = licenseAutoDiscovery;
    }

    public String getScopes() {
        return scopes;
    }

    public boolean isLicenseAutoDiscovery() {
        return licenseAutoDiscovery;
    }

    public void setRunChecks(boolean runChecks) {
        this.runChecks = runChecks;
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
        return details != null ? details.artifactoryName : null;
    }

    public String getRepositoryKey() {
        return details != null ? details.repositoryKey : null;
    }

    public String getArtifactoryUrl() {
        return details != null ? details.getArtifactoryUrl() : null;
    }

    public void setViolationRecipients(String violationRecipients) {
        this.violationRecipients = violationRecipients;
    }

    public String getViolationRecipients() {
        return violationRecipients;
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

    public boolean isBlackDuckRunChecks() {
        return blackDuckRunChecks;
    }

    public void setBlackDuckRunChecks(boolean blackDuckRunChecks) {
        this.blackDuckRunChecks = blackDuckRunChecks;
    }

    public String getBlackDuckAppName() {
        return blackDuckAppName;
    }

    public void setBlackDuckAppName(String blackDuckAppName) {
        this.blackDuckAppName = blackDuckAppName;
    }

    public String getBlackDuckAppVersion() {
        return blackDuckAppVersion;
    }

    public void setBlackDuckAppVersion(String blackDuckAppVersion) {
        this.blackDuckAppVersion = blackDuckAppVersion;
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

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject project) {
        return ActionableHelper.getArtifactoryProjectAction(details.getArtifactoryUrl(), project);
    }

    @Override
    public Environment setUp(final AbstractBuild build, Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException {
        File localDependencyFile = Which.jarFile(ArtifactoryBuildListener.class);
        final FilePath actualDependencyDir =
                PluginDependencyHelper.getActualDependencyDirectory(build, localDependencyFile);
        final PublisherContext context = new PublisherContext.Builder().artifactoryServer(getArtifactoryServer())
                .serverDetails(getDetails()).deployerOverrider(ArtifactoryIvyConfigurator.this).runChecks(isRunChecks())
                .includePublishArtifacts(isIncludePublishArtifacts()).violationRecipients(getViolationRecipients())
                .scopes(getScopes()).licenseAutoDiscovery(licenseAutoDiscovery).discardOldBuilds(isDiscardOldBuilds())
                .deployArtifacts(isDeployArtifacts()).includesExcludes(getArtifactDeploymentPatterns())
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .includeEnvVars(isIncludeEnvVars()).envVarsPatterns(getEnvVarsPatterns())
                .discardBuildArtifacts(isDiscardBuildArtifacts()).matrixParams(getMatrixParams())
                .artifactsPattern(getArtifactPattern()).ivyPattern(getIvyPattern()).maven2Compatible(isM2Compatible())
                .enableIssueTrackerIntegration(isEnableIssueTrackerIntegration())
                .aggregateBuildIssues(isAggregateBuildIssues()).aggregationBuildStatus(getAggregationBuildStatus())
                .integrateBlackDuck(isBlackDuckRunChecks(), getBlackDuckAppName(), getBlackDuckAppVersion(),
                        getBlackDuckReportRecipients(), getBlackDuckScopes(), isBlackDuckIncludePublishedArtifacts(),
                        isAutoCreateMissingComponentRequests(), isAutoDiscardStaleComponentRequests())
                .build();
        build.setResult(Result.SUCCESS);
        return new AntIvyBuilderEnvironment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                try {
                    ExtractorUtils.addBuilderInfoArguments(env, build, listener, context, null);
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
        List<ArtifactoryServer> servers = getDescriptor().getArtifactoryServers();
        for (ArtifactoryServer server : servers) {
            if (server.getName().equals(getArtifactoryName())) {
                return server;
            }
        }
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends BuildWrapperDescriptor {
        public DescriptorImpl() {
            super(ArtifactoryIvyConfigurator.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return "hudson.ivy.IvyModuleSet".equals(item.getClass().getName());
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
    private transient String password;
}
