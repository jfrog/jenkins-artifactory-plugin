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

    String getDefaultPromotionTargetRepository();

    boolean isIncludeEnvVars();

    IncludesExcludes getEnvVarsPatterns();

    boolean isDiscardOldBuilds();

    boolean isDiscardBuildArtifacts();

    boolean isAsyncBuildRetention();

    boolean isEnableIssueTrackerIntegration();

    boolean isAggregateBuildIssues();

    String getAggregationBuildStatus();

    String getCustomBuildName();

    boolean isOverrideBuildName();
}
