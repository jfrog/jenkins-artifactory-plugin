package org.jfrog.hudson;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.IssuesTrackerHelper;

import java.io.IOException;
import java.util.*;

/**
 * Handles build info creation and deployment
 *
 * @author Shay Yaakov
 */
public class AbstractBuildInfoDeployer {
    protected Run build;
    protected TaskListener listener;
    protected ArtifactoryBuildInfoClient client;
    private BuildInfoAwareConfigurator configurator;
    private EnvVars env;

    public AbstractBuildInfoDeployer(BuildInfoAwareConfigurator configurator, Run build,
                                     TaskListener listener, ArtifactoryBuildInfoClient client) throws IOException, InterruptedException {
        this.configurator = configurator;
        this.build = build;
        this.listener = listener;
        this.client = client;
        this.env = build.getEnvironment(listener);
    }

    protected Build createBuildInfo(String buildAgentName, String buildAgentVersion, BuildType buildType) {
        String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(configurator, build, env);
        BuildInfoBuilder builder = new BuildInfoBuilder(buildName)
                .number(BuildUniqueIdentifierHelper.getBuildNumber(build)).type(buildType)
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion())
                .buildAgent(new BuildAgent(buildAgentName, buildAgentVersion))
                .agent(new Agent("Jenkins", Jenkins.VERSION));
        String buildUrl = ActionableHelper.getBuildUrl(build);
        if (StringUtils.isNotBlank(buildUrl)) {
            builder.url(buildUrl);
        }

        Calendar startedTimestamp = build.getTimestamp();
        builder.startedDate(startedTimestamp.getTime());

        long duration = System.currentTimeMillis() - startedTimestamp.getTimeInMillis();
        builder.durationMillis(duration);

        String artifactoryPrincipal = configurator.getArtifactoryServer().getResolvingCredentialsConfig().provideCredentials(build.getParent()).getUsername();
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
        String url = ExtractorUtils.getVcsUrl(env);
        if (StringUtils.isNotBlank(url)) {
            builder.vcsUrl(url);
        }
        Vcs vcs = new Vcs(url, revision);
        if (!vcs.isEmpty()) {
            ArrayList<Vcs> vcsList = new ArrayList<>();
            vcsList.add(vcs);
            builder.vcs(vcsList);
        }

        addBuildInfoProperties(builder);

        if ((Jenkins.getInstance().getPlugin("jira") != null) && configurator.isEnableIssueTrackerIntegration()) {
            new IssuesTrackerHelper(build, listener, configurator.isAggregateBuildIssues(),
                    configurator.getAggregationBuildStatus()).setIssueTrackerInfo(builder);
        }

        // add staging status if it is a release build
        ReleaseAction release = ActionableHelper.getLatestAction(build, ReleaseAction.class);
        if (release != null) {
            String stagingRepoKey = release.getStagingRepositoryKey();
            if (StringUtils.isBlank(stagingRepoKey)) {
                stagingRepoKey = Util.replaceMacro(configurator.getRepositoryKey(), env);
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

    protected void addBuildInfoProperties(BuildInfoBuilder builder) {
        if (configurator.isIncludeEnvVars()) {
            IncludesExcludes envVarsPatterns = configurator.getEnvVarsPatterns();
            if (envVarsPatterns != null) {
                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        Util.replaceMacro(envVarsPatterns.getIncludePatterns(), env),
                        Util.replaceMacro(envVarsPatterns.getExcludePatterns(), env));
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
        if (!(build instanceof AbstractBuild)) {
            return;
        }

        Map<String, String> buildVariables = ((AbstractBuild) build).getBuildVariables();
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
