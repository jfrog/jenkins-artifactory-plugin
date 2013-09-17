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
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.MavenAbstractArtifactRecord;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.XStream2;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.action.ArtifactoryProjectAction;
import org.jfrog.hudson.maven2.ArtifactsDeployer;
import org.jfrog.hudson.maven2.MavenBuildInfoDeployer;
import org.jfrog.hudson.release.UnifiedPromoteBuildAction;
import org.jfrog.hudson.util.CredentialResolver;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.FormValidations;
import org.jfrog.hudson.util.IncludesExcludes;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
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
     * Repository URL and repository to deploy artifacts to.
     */
    private final ServerDetails details;
    /**
     * If checked (default) deploy maven artifacts
     */
    private final boolean deployArtifacts;
    private final IncludesExcludes artifactDeploymentPatterns;
    private final Credentials overridingDeployerCredentials;
    /**
     * Include environment variables in the generated build info
     */
    private final boolean includeEnvVars;

    private boolean deployBuildInfo;

    private final IncludesExcludes envVarsPatterns;
    /**
     * Deploy even if the build is unstable (failed tests)
     */
    public final boolean evenIfUnstable;

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
    private String aggregationBuildStatus;
    private final boolean enableIssueTrackerIntegration;
    private boolean aggregateBuildIssues;
    private final boolean allowPromotionOfNonStagedBuilds;

    private boolean blackDuckRunChecks;
    private String blackDuckAppName;
    private String blackDuckAppVersion;
    private String blackDuckReportRecipients; //csv
    private String blackDuckScopes; //csv
    private boolean blackDuckIncludePublishedArtifacts;
    private boolean autoCreateMissingComponentRequests;
    private boolean autoDiscardStaleComponentRequests;

    @DataBoundConstructor
    public ArtifactoryRedeployPublisher(ServerDetails details, boolean deployArtifacts,
            IncludesExcludes artifactDeploymentPatterns, Credentials overridingDeployerCredentials,
            boolean includeEnvVars, IncludesExcludes envVarsPatterns,
            boolean deployBuildInfo, boolean evenIfUnstable, boolean runChecks,
            String violationRecipients, boolean includePublishArtifacts, String scopes,
            boolean disableLicenseAutoDiscovery, boolean discardOldBuilds, boolean passIdentifiedDownstream,
            boolean discardBuildArtifacts, String matrixParams, boolean enableIssueTrackerIntegration,
            boolean aggregateBuildIssues, String aggregationBuildStatus, boolean allowPromotionOfNonStagedBuilds,
            boolean blackDuckRunChecks, String blackDuckAppName, String blackDuckAppVersion,
            String blackDuckReportRecipients, String blackDuckScopes, boolean blackDuckIncludePublishedArtifacts,
            boolean autoCreateMissingComponentRequests, boolean autoDiscardStaleComponentRequests) {
        this.details = details;
        this.deployArtifacts = deployArtifacts;
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
        this.overridingDeployerCredentials = overridingDeployerCredentials;
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
        this.licenseAutoDiscovery = !disableLicenseAutoDiscovery;
        this.deployBuildInfo = deployBuildInfo;
        this.enableIssueTrackerIntegration = enableIssueTrackerIntegration;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.allowPromotionOfNonStagedBuilds = allowPromotionOfNonStagedBuilds;
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

    public boolean isOverridingDefaultDeployer() {
        return (getOverridingDeployerCredentials() != null);
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
        return details != null ? details.getArtifactoryUrl() : null;
    }

    /**
     * @return The release versions deployment repository.
     */
    public String getRepositoryKey() {
        return details != null ? details.repositoryKey : null;
    }

    /**
     * @return The snapshots deployment repository. If not defined the releases deployment repository will be returned
     */
    public String getSnapshotsRepositoryKey() {
        return details != null ?
                (details.snapshotsRepositoryKey != null ? details.snapshotsRepositoryKey : details.repositoryKey) :
                null;
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

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return details != null ? new ArtifactoryProjectAction(details.getArtifactoryUrl(), project) : null;
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

        if (isExtractorUsed(build.getEnvironment(listener))) {
            if (deployBuildInfo) {
                build.getActions().add(0, new BuildInfoResultAction(getArtifactoryUrl(), build));
                if (isAllowPromotionOfNonStagedBuilds()) {
                    build.getActions().add(new UnifiedPromoteBuildAction<ArtifactoryRedeployPublisher>(build, this));
                }
            }
            return true;
        }

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
        Credentials preferredDeployer = CredentialResolver.getPreferredDeployer(this, server);
        ArtifactoryBuildInfoClient client = server.createArtifactoryClient(preferredDeployer.getUsername(),
                preferredDeployer.getPassword(), server.createProxyConfiguration(Jenkins.getInstance().proxy));
        try {
            verifySupportedArtifactoryVersion(client);
            if (deployArtifacts) {
                new ArtifactsDeployer(this, client, mavenBuild, listener).deploy();
            }
            if (deployBuildInfo) {
                new MavenBuildInfoDeployer(this, client, mavenBuild, listener).deploy();
                // add the result action (prefer always the same index)
                build.getActions().add(0, new BuildInfoResultAction(getArtifactoryUrl(), build));
                if (isAllowPromotionOfNonStagedBuilds()) {
                    build.getActions().add(new UnifiedPromoteBuildAction<ArtifactoryRedeployPublisher>(build, this));
                }
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

    private boolean isBuildFromM2ReleasePlugin(AbstractBuild<?, ?> build) {
        List<Cause> causes = build.getCauses();
        return !causes.isEmpty() && Iterables.any(causes, new Predicate<Cause>() {
            public boolean apply(Cause input) {
                return "org.jvnet.hudson.plugins.m2release.ReleaseCause".equals(input.getClass().getName());
            }
        });
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
        List<ArtifactoryServer> servers = getDescriptor().getArtifactoryServers();
        for (ArtifactoryServer server : servers) {
            //support legacy code when artifactoryName was the url
            if (server.getName().equals(getArtifactoryName()) || server.getUrl().equals(getArtifactoryName())) {
                return server;
            }
        }
        return null;
    }

    private Result getTreshold() {
        if (evenIfUnstable) {
            return Result.UNSTABLE;
        } else {
            return Result.SUCCESS;
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType == MavenModuleSet.class;
        }

        @Override
        public ArtifactoryRedeployPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ArtifactoryRedeployPublisher publisher = req.bindJSON(ArtifactoryRedeployPublisher.class, formData);
            if (formData.has("details")) {
                JSONObject serverDetails = formData.getJSONObject("details");
                if (serverDetails.has("stagingPlugin")) {
                    PluginSettings settings = new PluginSettings();
                    Map<String, String> paramMap = Maps.newHashMap();
                    JSONObject pluginSettings = serverDetails.getJSONObject("stagingPlugin");
                    String pluginName = pluginSettings.getString("pluginName");
                    settings.setPluginName(pluginName);
                    Map<String, Object> filteredPluginSettings = Maps.filterKeys(pluginSettings,
                            new Predicate<String>() {
                                public boolean apply(String input) {
                                    return StringUtils.isNotBlank(input) && !"pluginName".equals(input);
                                }
                            });
                    for (Map.Entry<String, Object> settingsEntry : filteredPluginSettings.entrySet()) {
                        String key = settingsEntry.getKey();
                        paramMap.put(key, pluginSettings.getString(key));
                    }
                    if (!paramMap.isEmpty()) {
                        settings.setParamMap(paramMap);
                    }
                    publisher.details.setStagingPlugin(settings);
                }
            }
            return publisher;
        }

        @Override
        public String getDisplayName() {
            return "Deploy artifacts to Artifactory";
            //return Messages.RedeployPublisher_getDisplayName();
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
            ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl)
                    Hudson.getInstance().getDescriptor(ArtifactoryBuilder.class);
            return descriptor.getArtifactoryServers();
        }

        public boolean isJiraPluginEnabled() {
            return (Jenkins.getInstance().getPlugin("jira") != null);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static final class ConverterImpl extends XStream2.PassthruConverter<ArtifactoryRedeployPublisher> {

        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }

        @Override
        protected void callback(ArtifactoryRedeployPublisher publisher, UnmarshallingContext context) {
            // field skipBuildInfoDeploy was replaced by the opposite deployBuildInfo
            if (publisher.skipBuildInfoDeploy != null) {
                publisher.deployBuildInfo = !publisher.skipBuildInfoDeploy;
            }
        }
    }

    /**
     * @deprecated: Use org.jfrog.hudson.ArtifactoryRedeployPublisher#deployBuildInfo
     */
    @Deprecated
    private transient Boolean skipBuildInfoDeploy;

}
