package org.jfrog.hudson;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.BlackDuckProperties;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Governance;
import org.jfrog.build.api.LicenseControl;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.util.BuildRetentionFactory;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.IssuesTrackerHelper;

import java.io.IOException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Handles build info creation and deployment
 *
 * @author Shay Yaakov
 */
public class AbstractBuildInfoDeployer {
    private BuildInfoAwareConfigurator configurator;
    protected AbstractBuild build;
    protected BuildListener listener;
    protected ArtifactoryBuildInfoClient client;
    private EnvVars env;

    public AbstractBuildInfoDeployer(BuildInfoAwareConfigurator configurator, AbstractBuild build,
            BuildListener listener, ArtifactoryBuildInfoClient client) throws IOException, InterruptedException {
        this.configurator = configurator;
        this.build = build;
        this.listener = listener;
        this.client = client;
        this.env = build.getEnvironment(listener);
    }

    protected Build createBuildInfo(String buildAgentName, String buildAgentVersion, BuildType buildType) {
        BuildInfoBuilder builder = new BuildInfoBuilder(
                ExtractorUtils.sanitizeBuildName(build.getParent().getFullName()))
                .number(build.getNumber() + "").type(buildType)
                .buildAgent(new BuildAgent(buildAgentName, buildAgentVersion))
                .agent(new Agent("hudson", build.getHudsonVersion()));
        String buildUrl = ActionableHelper.getBuildUrl(build);
        if (StringUtils.isNotBlank(buildUrl)) {
            builder.url(buildUrl);
        }

        Calendar startedTimestamp = build.getTimestamp();
        builder.startedDate(startedTimestamp.getTime());

        long duration = System.currentTimeMillis() - startedTimestamp.getTimeInMillis();
        builder.durationMillis(duration);

        String artifactoryPrincipal = configurator.getArtifactoryServer().getResolvingCredentials().getUsername();
        if (StringUtils.isBlank(artifactoryPrincipal)) {
            artifactoryPrincipal = "";
        }
        builder.artifactoryPrincipal(artifactoryPrincipal);

        String userCause = ActionableHelper.getUserCausePrincipal(build);
        if (userCause != null) {
            builder.principal(userCause);
        }

        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            String parentProject = ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject());
            int parentNumber = parent.getUpstreamBuild();
            builder.parentName(parentProject);
            builder.parentNumber(parentNumber + "");
            if (StringUtils.isBlank(userCause)) {
                builder.principal("auto");
            }
        }

        String revision = ExtractorUtils.getVcsRevision(env);
        if (StringUtils.isNotBlank(revision)) {
            builder.vcsRevision(revision);
        }

        addBuildInfoProperties(builder);

        LicenseControl licenseControl = new LicenseControl(configurator.isRunChecks());
        if (configurator.isRunChecks()) {
            if (StringUtils.isNotBlank(configurator.getViolationRecipients())) {
                licenseControl.setLicenseViolationsRecipientsList(configurator.getViolationRecipients());
            }
            if (StringUtils.isNotBlank(configurator.getScopes())) {
                licenseControl.setScopesList(configurator.getScopes());
            }
        }
        licenseControl.setIncludePublishedArtifacts(configurator.isIncludePublishArtifacts());
        licenseControl.setAutoDiscover(configurator.isLicenseAutoDiscovery());
        builder.licenseControl(licenseControl);

        BlackDuckProperties blackDuckProperties = new BlackDuckProperties();
        blackDuckProperties.setRunChecks(configurator.isBlackDuckRunChecks());
        blackDuckProperties.setAppName(configurator.getBlackDuckAppName());
        blackDuckProperties.setAppVersion(configurator.getBlackDuckAppVersion());
        blackDuckProperties.setReportRecipients(configurator.getBlackDuckReportRecipients());
        blackDuckProperties.setScopes(configurator.getBlackDuckScopes());
        blackDuckProperties.setIncludePublishedArtifacts(configurator.isBlackDuckIncludePublishedArtifacts());
        blackDuckProperties.setAutoCreateMissingComponentRequests(configurator.isAutoCreateMissingComponentRequests());
        blackDuckProperties.setAutoDiscardStaleComponentRequests(configurator.isAutoDiscardStaleComponentRequests());

        Governance governance = new Governance();
        governance.setBlackDuckProperties(blackDuckProperties);
        builder.governance(governance);

        BuildRetention buildRetention = new BuildRetention(configurator.isDiscardBuildArtifacts());
        if (configurator.isDiscardOldBuilds()) {
            buildRetention = BuildRetentionFactory.createBuildRetention(build, configurator.isDiscardBuildArtifacts());
        }
        builder.buildRetention(buildRetention);

        if ((Jenkins.getInstance().getPlugin("jira") != null) && configurator.isEnableIssueTrackerIntegration()) {
            new IssuesTrackerHelper(build, listener, configurator.isAggregateBuildIssues(),
                    configurator.getAggregationBuildStatus()).setIssueTrackerInfo(builder);
        }

        // add staging status if it is a release build
        ReleaseAction release = ActionableHelper.getLatestAction(build, ReleaseAction.class);
        if (release != null) {
            String stagingRepoKey = release.getStagingRepositoryKey();
            if (StringUtils.isBlank(stagingRepoKey)) {
                stagingRepoKey = configurator.getRepositoryKey();
            }
            builder.addStatus(new PromotionStatusBuilder(Promotion.STAGED)
                    .timestampDate(startedTimestamp.getTime())
                    .comment(release.getStagingComment())
                    .repository(stagingRepoKey)
                    .ciUser(userCause).user(artifactoryPrincipal).build());
        }

        Build buildInfo = builder.build();
        // for backwards compatibility for Artifactory 2.2.3
        if (parent != null) {
            buildInfo.setParentBuildId(parent.getUpstreamProject());
        }

        return buildInfo;
    }

    private void addBuildInfoProperties(BuildInfoBuilder builder) {
        if (configurator.isIncludeEnvVars()) {
            IncludesExcludes envVarsPatterns = configurator.getEnvVarsPatterns();
            if (envVarsPatterns != null) {
                IncludeExcludePatterns patterns = new IncludeExcludePatterns(envVarsPatterns.getIncludePatterns(),
                        envVarsPatterns.getExcludePatterns());
                // First add all build related variables
                addBuildVariables(builder, patterns);

                // Then add env variables
                addEnvVariables(builder, patterns);

                // And finally add system variables
                addSystemVariables(builder, patterns);
            }
        }
    }

    private void addBuildVariables(BuildInfoBuilder builder, IncludeExcludePatterns patterns) {
        Map<String, String> buildVariables = build.getBuildVariables();
        for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
            String varKey = entry.getKey();
            if (PatternMatcher.pathConflicts(varKey, patterns)) {
                continue;
            }
            builder.addProperty(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + varKey, entry.getValue());
        }
    }

    private void addEnvVariables(BuildInfoBuilder builder, IncludeExcludePatterns patterns) {
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String varKey = entry.getKey();
            if (PatternMatcher.pathConflicts(varKey, patterns)) {
                continue;
            }
            builder.addProperty(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + varKey, entry.getValue());
        }
    }

    private void addSystemVariables(BuildInfoBuilder builder, IncludeExcludePatterns patterns) {
        Properties systemProperties = System.getProperties();
        Enumeration<?> enumeration = systemProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String propertyKey = (String) enumeration.nextElement();
            if (PatternMatcher.pathConflicts(propertyKey, patterns)) {
                continue;
            }
            builder.addProperty(propertyKey, systemProperties.getProperty(propertyKey));
        }
    }
}
