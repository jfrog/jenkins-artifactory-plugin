package org.jfrog.hudson;

import org.jfrog.hudson.util.IncludesExcludes;

/**
 * Represents a class that can be passed to {@link AbstractBuildInfoDeployer} for build info creation
 *
 * @author Shay Yaakov
 */
public interface BuildInfoAwareConfigurator {

    ArtifactoryServer getArtifactoryServer();

    String getRepositoryKey();

    boolean isIncludeEnvVars();

    IncludesExcludes getEnvVarsPatterns();

    boolean isRunChecks();

    String getViolationRecipients();

    boolean isIncludePublishArtifacts();

    String getScopes();

    boolean isLicenseAutoDiscovery();

    boolean isDiscardOldBuilds();

    boolean isDiscardBuildArtifacts();

    boolean isEnableIssueTrackerIntegration();

    boolean isAggregateBuildIssues();

    String getAggregationBuildStatus();

    boolean isBlackDuckRunChecks();

    String getBlackDuckAppName();

    String getBlackDuckAppVersion();

    String getBlackDuckReportRecipients();

    String getBlackDuckScopes();

    boolean isBlackDuckIncludePublishedArtifacts();

    boolean isAutoCreateMissingComponentRequests();

    boolean isAutoDiscardStaleComponentRequests();
}
