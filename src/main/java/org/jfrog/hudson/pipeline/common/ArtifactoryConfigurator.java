package org.jfrog.hudson.pipeline.common;

import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.BuildInfoAwareConfigurator;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.IncludesExcludes;

/**
 * This class will be held with each new build info created in the pipeline.
 * So we know what to collect to the build info.
 */
public class ArtifactoryConfigurator implements BuildInfoAwareConfigurator, DeployerOverrider {

    private ArtifactoryServer server;

    public ArtifactoryConfigurator(ArtifactoryServer server) {
        this.server = server;
    }

    public void setServer(ArtifactoryServer server) {
        this.server = server;
    }

    public ArtifactoryServer getArtifactoryServer() {
        return server;
    }

    public String getRepositoryKey() {
        return null;
    }

    public String getDefaultPromotionTargetRepository() {
        return null;
    }

    public boolean isIncludeEnvVars() {
        return false;
    }

    public IncludesExcludes getEnvVarsPatterns() {
        return null;
    }

    public boolean isRunChecks() {
        return false;
    }

    public String getViolationRecipients() {
        return null;
    }

    public boolean isIncludePublishArtifacts() {
        return false;
    }

    public String getScopes() {
        return null;
    }

    public boolean isLicenseAutoDiscovery() {
        return false;
    }

    public boolean isDiscardOldBuilds() {
        return false;
    }

    public boolean isDiscardBuildArtifacts() {
        return false;
    }

    public boolean isAsyncBuildRetention() {
        return false;
    }

    public boolean isEnableIssueTrackerIntegration() {
        return false;
    }

    public boolean isAggregateBuildIssues() {
        return false;
    }

    public String getAggregationBuildStatus() {
        return null;
    }

    public boolean isBlackDuckRunChecks() {
        return false;
    }

    public String getBlackDuckAppName() {
        return null;
    }

    public String getBlackDuckAppVersion() {
        return null;
    }

    public String getBlackDuckReportRecipients() {
        return null;
    }

    public String getBlackDuckScopes() {
        return null;
    }

    public boolean isBlackDuckIncludePublishedArtifacts() {
        return false;
    }

    public boolean isAutoCreateMissingComponentRequests() {
        return false;
    }

    public boolean isAutoDiscardStaleComponentRequests() {
        return false;
    }

    public boolean isOverridingDefaultDeployer() {
        return false;
    }

    public Credentials getOverridingDeployerCredentials() {
        return null;
    }

    public CredentialsConfig getDeployerCredentialsConfig() {
        return null;
    }

    public String getCustomBuildName() {
        return "";
    }

    public boolean isOverrideBuildName() {
        return false;
    }
}
