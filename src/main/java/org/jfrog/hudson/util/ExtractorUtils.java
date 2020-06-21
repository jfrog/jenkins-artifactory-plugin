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

package org.jfrog.hudson.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.*;
import hudson.slaves.SlaveComputer;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator;
import org.jfrog.hudson.maven3.ArtifactoryMaven3NativeConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.resolvers.MavenResolver;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.util.plugins.MultiConfigurationUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

import java.io.*;
import java.util.*;

/**
 * @author Tomer Cohen
 */
public class ExtractorUtils {

    /**
     * Flag to indicate whether an external extractor was used, and the work doesn't need to be done from inside
     * Jenkins.
     */
    public static final String EXTRACTOR_USED = "extractor.used";
    public static final String GIT_COMMIT = "GIT_COMMIT";
    public static final String GIT_URL = "GIT_URL";

    private ExtractorUtils() {
        // utility class
        throw new IllegalAccessError();
    }

    /**
     * Get the VCS revision from the Jenkins build environment. The search will one of "SVN_REVISION", "GIT_COMMIT",
     * "P4_CHANGELIST" in the environment.
     *
     * @param env Th Jenkins build environment.
     * @return The vcs revision for supported VCS
     */
    public static String getVcsRevision(Map<String, String> env) {
        String revision = env.get("SVN_REVISION");
        if (StringUtils.isBlank(revision)) {
            revision = env.get(GIT_COMMIT);
        }
        if (StringUtils.isBlank(revision)) {
            revision = env.get("P4_CHANGELIST");
        }
        return revision;
    }

    /**
     * Get the VCS url from the Jenkins build environment. The search will one of "SVN_REVISION", "GIT_COMMIT",
     * "P4_CHANGELIST" in the environment.
     *
     * @param env Th Jenkins build environment.
     * @return The vcs url for supported VCS
     */
    public static String getVcsUrl(Map<String, String> env) {
        String url = env.get("SVN_URL");
        if (StringUtils.isBlank(url)) {
            url = publicGitUrl(env.get(GIT_URL));
        }
        if (StringUtils.isBlank(url)) {
            url = env.get("P4PORT");
        }
        return url;
    }

    public static void addVcsDetailsToEnv(FilePath filePath, EnvVars env, TaskListener listener) throws IOException, InterruptedException {
        Vcs vcs = Utils.extractVcs(filePath, new JenkinsBuildInfoLog(listener));
        env.put(GIT_COMMIT, StringUtils.defaultIfEmpty(vcs.getRevision(), ""));
        env.put(GIT_URL, StringUtils.defaultIfEmpty(vcs.getUrl(), ""));
    }

    /*
     *   Git publish the repository credentials in the Url,
     *   this method will discard it.
     */
    private static String publicGitUrl(String gitUrl) {
        if (gitUrl != null && gitUrl.contains("https://") && gitUrl.contains("@")) {
            StringBuilder sb = new StringBuilder(gitUrl);
            int start = sb.indexOf("https://");
            int end = sb.indexOf("@") + 1;
            sb = sb.replace(start, end, StringUtils.EMPTY);

            return "https://" + sb.toString();
        }

        return gitUrl;
    }

    /**
     * Add build info properties that will be read by an external extractor. All properties are then saved into a {@code
     * buildinfo.properties} into a temporary location. The location is then put into an environment variable {@link
     * BuildInfoConfigProperties#PROP_PROPS_FILE} for the extractor to read.
     *
     * @param env              A map of the environment variables that are to be persisted into the buildinfo.properties
     *                         file. NOTE: nothing should be added to the env in this method
     * @param build            The build from which to get build/project related information from (e.g build name and
     *                         build number).
     * @param listener
     * @param publisherContext A context for publisher settings
     * @param resolverContext  A context for resolver settings
     */
    public static ArtifactoryClientConfiguration addBuilderInfoArguments(Map<String, String> env, Run build, TaskListener listener,
                                                                         PublisherContext publisherContext, ResolverContext resolverContext, FilePath ws, hudson.Launcher launcher)
            throws Exception {
        ArtifactoryClientConfiguration configuration = getArtifactoryClientConfiguration(env, build,
                null, listener, publisherContext, resolverContext);
        if (isMavenResolutionConfigured(resolverContext)) {
            env.put(BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED, Boolean.TRUE.toString());
        }

        // Create tempdir for properties file
        FilePath tempDir = createAndGetTempDir(ws);

        persistConfiguration(configuration, env, tempDir, launcher);
        return configuration;
    }

    public static ArtifactoryClientConfiguration getArtifactoryClientConfiguration(Map<String, String> env, Run build,
                                                                                   BuildInfo pipelineBuildInfo, TaskListener listener, PublisherContext publisherContext, ResolverContext resolverContext) throws IOException {
        ArtifactoryClientConfiguration configuration = new ArtifactoryClientConfiguration(new NullLog());
        if (build instanceof AbstractBuild) {
            addBuildRootIfNeeded((AbstractBuild) build, configuration);
        }

        if (publisherContext != null) {
            setPublisherInfo(env, build, pipelineBuildInfo, publisherContext, configuration);
            publisherContext.setArtifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion());
        }

        if (resolverContext != null) {
            setResolverInfo(configuration, build, resolverContext, env);
        }

        if (!shouldBypassProxy(resolverContext, publisherContext)) {
            setProxy(configuration);
        }

        if ((Jenkins.getInstance().getPlugin("jira") != null) && (publisherContext != null) &&
                publisherContext.isEnableIssueTrackerIntegration()) {
            new IssuesTrackerHelper(build, listener, publisherContext.isAggregateBuildIssues(),
                    publisherContext.getAggregationBuildStatus()).setIssueTrackerInfo(configuration);
        }

        IncludesExcludes envVarsPatterns = new IncludesExcludes("", "");
        if (publisherContext != null && publisherContext.getEnvVarsPatterns() != null) {
            envVarsPatterns = publisherContext.getEnvVarsPatterns();
        }
        addEnvVars(env, build, configuration, envVarsPatterns, listener);
        return configuration;
    }

    private static boolean shouldBypassProxy(ResolverContext resolverContext, PublisherContext publisherContext) {
        boolean bypass =
                resolverContext != null && resolverContext.getServer() != null
                        && resolverContext.getServer().isBypassProxy();
        return bypass ||
                publisherContext != null && publisherContext.getArtifactoryServer() != null
                        && publisherContext.getArtifactoryServer().isBypassProxy();
    }

    private static void setProxy(ArtifactoryClientConfiguration configuration) {
        Jenkins j = Jenkins.getInstance();
        if (j.proxy != null) {
            configuration.proxy.setHost(j.proxy.name);
            configuration.proxy.setPort(j.proxy.port);
            configuration.proxy.setUsername(j.proxy.getUserName());
            configuration.proxy.setPassword(j.proxy.getPassword());
        }
    }

    private static void setResolverInfo(ArtifactoryClientConfiguration configuration, Run build,
                                        ResolverContext context, Map<String, String> env) throws java.io.IOException {
        configuration.setTimeout(context.getServer().getTimeout());
        setRetryParams(configuration, context.getServer());
        configuration.resolver.setContextUrl(context.getServerDetails().getArtifactoryUrl());
        String inputDownloadReleaseKey = context.getServerDetails().getResolveReleaseRepository().getRepoKey();
        String inputDownloadSnapshotKey = context.getServerDetails().getResolveSnapshotRepository().getRepoKey();
        // These input variables might be a variable that should be replaced with it's value
        replaceRepositoryInputForValues(configuration, build, inputDownloadReleaseKey, inputDownloadSnapshotKey, env);
        CredentialsConfig preferredResolver = CredentialManager.getPreferredResolver(context.getResolverOverrider(),
                context.getServer());
        Credentials resolverCredentials = preferredResolver.provideCredentials(build.getParent());
        if (StringUtils.isNotEmpty(resolverCredentials.getAccessToken())) {
            resolverCredentials = resolverCredentials.convertAccessTokenToUsernamePassword();
        }
        if (StringUtils.isNotBlank(resolverCredentials.getUsername())) {
            configuration.resolver.setUsername(resolverCredentials.getUsername());
            configuration.resolver.setPassword(resolverCredentials.getPassword());
        }
    }

    /*
     * If necessary, replace the input for the configured repositories to their values
     * under the current environment. We are not allowing for the input or the value to be empty.
     */
    private static void replaceRepositoryInputForValues(ArtifactoryClientConfiguration configuration,
                                                        Run build, String resolverReleaseInput,
                                                        String resolverSnapshotInput, Map<String, String> env) {
        if (StringUtils.isBlank(resolverReleaseInput) || StringUtils.isBlank(resolverSnapshotInput)) {
            throw new IllegalStateException("Input for resolve repositories cannot be empty.");
        }
        String resolveReleaseRepo = Util.replaceMacro(resolverReleaseInput, env);
        String resolveSnapshotRepo = Util.replaceMacro(resolverSnapshotInput, env);
        if (StringUtils.isBlank(resolveReleaseRepo) || StringUtils.isBlank(resolveSnapshotRepo)) {
            throw new IllegalStateException("Resolver repository variable cannot be replaces with empty value.");
        }
        configuration.resolver.setDownloadSnapshotRepoKey(resolveSnapshotRepo);
        configuration.resolver.setRepoKey(resolveReleaseRepo);
    }

    /**
     * Set all the parameters relevant for publishing artifacts and build info
     */
    private static void setPublisherInfo(Map<String, String> env, Run build, BuildInfo pipelineBuildInfo, PublisherContext context,
                                         ArtifactoryClientConfiguration configuration) throws IOException {
        configuration.setActivateRecorder(Boolean.TRUE);
        String buildName;
        String buildNumber;
        if (pipelineBuildInfo != null) {
            buildName = pipelineBuildInfo.getName();
            buildNumber = pipelineBuildInfo.getNumber();
        } else {
            buildName = context.isOverrideBuildName() ? context.getCustomBuildName() : BuildUniqueIdentifierHelper.getBuildName(build);
            buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
        }

        configuration.info.setBuildName(buildName);
        configuration.publisher.addMatrixParam(BuildInfoFields.BUILD_NAME, buildName);
        configuration.info.setBuildNumber(buildNumber);
        configuration.publisher.addMatrixParam(BuildInfoFields.BUILD_NUMBER, buildNumber);
        configuration.info.setArtifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion());

        Date buildStartDate = build.getTimestamp().getTime();
        configuration.info.setBuildStarted(buildStartDate.getTime());
        configuration.info.setBuildTimestamp(String.valueOf(build.getStartTimeInMillis()));
        configuration.publisher.addMatrixParam(BuildInfoFields.BUILD_TIMESTAMP, String.valueOf(build.getStartTimeInMillis()));

        String vcsRevision = getVcsRevision(env);
        if (StringUtils.isNotBlank(vcsRevision)) {
            configuration.info.setVcsRevision(vcsRevision);
            configuration.publisher.addMatrixParam(BuildInfoFields.VCS_REVISION, vcsRevision);
        }

        String vcsUrl = getVcsUrl(env);
        if (StringUtils.isNotBlank(vcsUrl)) {
            configuration.info.setVcsUrl(vcsUrl);
            configuration.publisher.addMatrixParam(BuildInfoFields.VCS_URL, vcsUrl);
        }

        if (StringUtils.isNotBlank(context.getArtifactsPattern())) {
            configuration.publisher.setIvyArtifactPattern(Util.replaceMacro(context.getArtifactsPattern(), env));
        }
        if (StringUtils.isNotBlank(context.getIvyPattern())) {
            configuration.publisher.setIvyPattern(Util.replaceMacro(context.getIvyPattern(), env));
        }
        configuration.publisher.setM2Compatible(context.isMaven2Compatible());
        String buildUrl = ActionableHelper.getBuildUrl(build);
        if (StringUtils.isNotBlank(buildUrl)) {
            configuration.info.setBuildUrl(buildUrl);
        }

        String userName = null;
        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            String parentProject = sanitizeBuildName(parent.getUpstreamProject());
            configuration.info.setParentBuildName(parentProject);
            configuration.publisher.addMatrixParam(BuildInfoFields.BUILD_PARENT_NAME, parentProject);
            String parentBuildNumber = parent.getUpstreamBuild() + "";
            configuration.info.setParentBuildNumber(parentBuildNumber);
            configuration.publisher.addMatrixParam(BuildInfoFields.BUILD_PARENT_NUMBER, parentBuildNumber);
            userName = "auto";
        }

        userName = ActionableHelper.getUserCausePrincipal(build, userName);

        configuration.info.setPrincipal(userName);
        configuration.info.setAgentName("Jenkins");
        configuration.info.setAgentVersion(Jenkins.VERSION);
        ArtifactoryServer artifactoryServer = context.getArtifactoryServer();
        if (artifactoryServer != null) {
            CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(context.getDeployerOverrider(), artifactoryServer);
            Credentials deployerCredentials = preferredDeployer.provideCredentials(build.getParent());
            if (StringUtils.isNotEmpty(deployerCredentials.getAccessToken())) {
                deployerCredentials = deployerCredentials.convertAccessTokenToUsernamePassword();
            }
            if (StringUtils.isNotBlank(deployerCredentials.getUsername())) {
                configuration.publisher.setUsername(deployerCredentials.getUsername());
                configuration.publisher.setPassword(deployerCredentials.getPassword());
            }
            configuration.setTimeout(artifactoryServer.getTimeout());
            setRetryParams(configuration, artifactoryServer);

            configuration.publisher.setContextUrl(artifactoryServer.getUrl());
        }

        ServerDetails serverDetails = context.getServerDetails();
        if (serverDetails != null) {
            String inputRepKey = serverDetails.getDeployReleaseRepositoryKey();
            String repoKEy = Util.replaceMacro(inputRepKey, env);
            configuration.publisher.setRepoKey(repoKEy);
            String inputSnapshotRepKey = serverDetails.getDeploySnapshotRepositoryKey();
            String snapshotRepoKey = Util.replaceMacro(inputSnapshotRepKey, env);
            configuration.publisher.setSnapshotRepoKey(snapshotRepoKey);
        }

        if (context.isDiscardOldBuilds()) {
            BuildRetention buildRetention = BuildRetentionFactory.createBuildRetention(build, context.isDiscardBuildArtifacts());
            if (buildRetention.getCount() > -1) {
                configuration.info.setBuildRetentionCount(buildRetention.getCount());
            }
            if (buildRetention.getMinimumBuildDate() != null) {
                long days = daysBetween(buildRetention.getMinimumBuildDate(), new Date());
                configuration.info.setBuildRetentionMinimumDate(String.valueOf(days));
            }
            configuration.info.setDeleteBuildArtifacts(context.isDiscardBuildArtifacts());
            configuration.info.setAsyncBuildRetention(context.isAsyncBuildRetention());
            configuration.info.setBuildNumbersNotToDelete(getBuildNumbersNotToBeDeletedAsString(build));
        }
        configuration.publisher.setPublishArtifacts(context.isDeployArtifacts());
        configuration.publisher.setPublishForkCount(context.getThreads());
        configuration.publisher.setEvenUnstable(context.isEvenIfUnstable());
        if (context.isDeployIvy() != null) {
            configuration.publisher.setIvy(context.isDeployIvy());
        }
        if (context.isDeployMaven() != null) {
            configuration.publisher.setMaven(context.isDeployMaven());
        }
        IncludesExcludes deploymentPatterns = context.getIncludesExcludes();
        if (deploymentPatterns != null) {
            String includePatterns = deploymentPatterns.getIncludePatterns();
            if (StringUtils.isNotBlank(includePatterns)) {
                configuration.publisher.setIncludePatterns(Util.replaceMacro(includePatterns, env));
            }
            String excludePatterns = deploymentPatterns.getExcludePatterns();
            if (StringUtils.isNotBlank(excludePatterns)) {
                configuration.publisher.setExcludePatterns(Util.replaceMacro(excludePatterns, env));
            }
        }
        ReleaseAction releaseAction = ActionableHelper.getLatestAction(build, ReleaseAction.class);
        if (releaseAction != null) {
            configuration.info.setReleaseEnabled(true);
            String comment = releaseAction.getStagingComment();
            if (StringUtils.isNotBlank(comment)) {
                configuration.info.setReleaseComment(comment);
            }
        }
        configuration.publisher.setFilterExcludedArtifactsFromBuild(context.isFilterExcludedArtifactsFromBuild());
        configuration.publisher.setPublishBuildInfo(!context.isSkipBuildInfoDeploy());
        configuration.publisher.setRecordAllDependencies(context.isRecordAllDependencies());
        configuration.setIncludeEnvVars(context.isIncludeEnvVars());
        IncludesExcludes envVarsPatterns = context.getEnvVarsPatterns();
        if (envVarsPatterns != null) {
            configuration.setEnvVarsIncludePatterns(Util.replaceMacro(envVarsPatterns.getIncludePatterns(), env));
            configuration.setEnvVarsExcludePatterns(Util.replaceMacro(envVarsPatterns.getExcludePatterns(), env));
        }
        addDeploymentProperties(context, configuration.publisher, env);
    }

    private static void setRetryParams(ArtifactoryClientConfiguration configuration, ArtifactoryServer artifactoryServer) {
        configuration.setConnectionRetries(artifactoryServer.getConnectionRetry());
    }

    // Naive implementation of the difference in days between two dates
    private static long daysBetween(Date date1, Date date2) {
        long diff;
        if (date2.after(date1)) {
            diff = date2.getTime() - date1.getTime();
        } else {
            diff = date1.getTime() - date2.getTime();
        }
        return diff / (24 * 60 * 60 * 1000);
    }

    /**
     * Replaces occurrences of '/' and '%2F' with ' :: ' if exist
     */
    public static String sanitizeBuildName(String buildName) {
        String s = StringUtils.replace(buildName, "/", " :: ");
        return StringUtils.replace(s, "%2F", " :: ");
    }

    /**
     * Get the list of build numbers that are to be kept forever.
     */
    public static List<String> getBuildNumbersNotToBeDeleted(Run build) {
        List<String> notToDelete = Lists.newArrayList();
        List<? extends Run<?, ?>> builds = build.getParent().getBuilds();
        for (Run<?, ?> run : builds) {
            if (run.isKeepLog()) {
                notToDelete.add(String.valueOf(run.getNumber()));
            }
        }
        return notToDelete;
    }

    private static String getBuildNumbersNotToBeDeletedAsString(Run build) {
        StringBuilder builder = new StringBuilder();
        List<String> notToBeDeleted = getBuildNumbersNotToBeDeleted(build);
        for (String notToDelete : notToBeDeleted) {
            builder.append(notToDelete).append(",");
        }
        return builder.toString();
    }

    public static void addBuildRootIfNeeded(AbstractBuild build, ArtifactoryClientConfiguration configuration)
            throws UnsupportedEncodingException {
        AbstractBuild<?, ?> rootBuild = BuildUniqueIdentifierHelper.getRootBuild(build);
        if (rootBuild != null) {
            String identifier = BuildUniqueIdentifierHelper.getUpstreamIdentifier(rootBuild);
            configuration.info.setBuildRoot(identifier);
        }
    }

    public static void persistConfiguration(ArtifactoryClientConfiguration configuration, Map<String, String> env, FilePath ws,
                                            hudson.Launcher launcher) throws IOException, InterruptedException {
        FilePath propertiesFile = ws.createTextTempFile("buildInfo", ".properties", "");
        ActionableHelper.deleteFilePathOnExit(propertiesFile);
        configuration.setPropertiesFile(propertiesFile.getRemote());
        env.put("BUILDINFO_PROPFILE", propertiesFile.getRemote());
        env.put(BuildInfoConfigProperties.PROP_PROPS_FILE, propertiesFile.getRemote());
        // Jenkins prefixes env variables with 'env' but we need it clean..
        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, propertiesFile.getRemote());
        if (!(getComputer(launcher) instanceof SlaveComputer)) {
            configuration.persistToPropertiesFile();
        } else {
            try {
                Properties properties = new Properties();
                properties.putAll(configuration.getAllRootConfig());
                properties.putAll(configuration.getAllProperties());
                OutputStream os = propertiesFile.write();
                try {
                    properties.store(os, "");
                } finally {
                    IOUtils.closeQuietly(os);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String buildPropertiesString(ArrayListMultimap<String, String> properties) {
        StringBuilder props = new StringBuilder();
        List<String> keys = new ArrayList<String>(properties.keySet());
        for (int i = 0; i < keys.size(); i++) {
            props.append(keys.get(i)).append("=");
            List<String> values = properties.get(keys.get(i));
            for (int j = 0; j < values.size(); j++) {
                props.append(values.get(j));
                if (j != values.size() - 1) {
                    props.append(",");
                }
            }
            if (i != keys.size() - 1) {
                props.append(";");
            }
        }
        return props.toString();
    }

    private static Computer getComputer(hudson.Launcher launcher) {
        Computer computer = Computer.currentComputer();
        if (computer != null) {
            return computer;
        } else {
            return Utils.getCurrentComputer(launcher);
        }
    }

    private static void addDeploymentProperties(PublisherContext context,
                                                ArtifactoryClientConfiguration.PublisherHandler publisher,
                                                Map<String, String> env) {
        String deploymentProperties = Util.replaceMacro(context.getDeploymentProperties(), env);
        ArrayListMultimap<String, String> params = ArrayListMultimap.create();
        SpecsHelper.fillPropertiesMap(deploymentProperties, params);
        publisher.addMatrixParams(params);
    }

    private static void addEnvVars(Map<String, String> env, Run<?, ?> build,
                                   ArtifactoryClientConfiguration configuration, IncludesExcludes envVarsPatterns, TaskListener listener) {
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                Util.replaceMacro(envVarsPatterns.getIncludePatterns(), env),
                Util.replaceMacro(envVarsPatterns.getExcludePatterns(), env)
        );

        // Add only the jenkins specific environment variables
        MapDifference<String, String> envDifference = Maps.difference(env, System.getenv());
        Map<String, String> filteredEnvDifference = envDifference.entriesOnlyOnLeft();
        configuration.info.addBuildVariables(filteredEnvDifference, patterns);

        // Add Jenkins build variables
        EnvVars buildVariables = getEnvVars(build, listener);
        MapDifference<String, String> buildVarDifference = Maps.difference(buildVariables, System.getenv());
        Map<String, String> filteredBuildVarDifferences = buildVarDifference.entriesOnlyOnLeft();
        configuration.info.addBuildVariables(filteredBuildVarDifferences, patterns);

        // Write all the deploy (matrix params) properties.
        configuration.fillFromProperties(buildVariables, patterns);
        for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
            if (entry.getKey().startsWith(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX)) {
                configuration.publisher.addMatrixParam(entry.getKey(), entry.getValue());
            }
        }

        MultiConfigurationUtils.addMatrixCombination(build, configuration);
    }

    private static EnvVars getEnvVars(Run<?, ?> build, TaskListener listener) {
        EnvVars buildVariables;
        if (build instanceof AbstractBuild) {
            buildVariables = new EnvVars();
            buildVariables.putAll(((AbstractBuild) build).getBuildVariables());
        } else {
            buildVariables = Utils.extractBuildParameters(build, listener);
        }
        return buildVariables;
    }

    /**
     * Converts the http entity to string. If entity is null, returns empty string.
     *
     * @param entity
     * @return
     * @throws IOException
     */
    public static String entityToString(HttpEntity entity) throws IOException {
        if (entity != null) {
            InputStream is = entity.getContent();
            return IOUtils.toString(is, "UTF-8");
        }
        return "";
    }

    /**
     * Validates that the String is not blank.
     *
     * @param content
     * @throws IOException - If the string is empty.
     */
    public static void validateStringNotBlank(String content) throws IOException {
        if (StringUtils.isBlank(content)) {
            throw new IOException("Received empty String.");
        }

    }

    private static boolean isMavenResolutionConfigured(ResolverContext resolverContext) {
        return resolverContext != null &&
                resolverContext.getResolverOverrider() != null &&
                (resolverContext.getResolverOverrider() instanceof ArtifactoryMaven3Configurator ||
                        resolverContext.getResolverOverrider() instanceof ArtifactoryMaven3NativeConfigurator ||
                        resolverContext.getResolverOverrider() instanceof MavenResolver);
    }

    /**
     * Create a temporary directory under a given workspace
     */
    public static FilePath createAndGetTempDir(final FilePath ws) throws IOException, InterruptedException {
        // The token that combines the project name and unique number to create unique workspace directory.
        String workspaceList = System.getProperty("hudson.slaves.WorkspaceList");
        return ws.act(new MasterToSlaveCallable<FilePath, IOException>() {
            @Override
            public FilePath call() {
                final FilePath tempDir = ws.sibling(ws.getName() + Objects.toString(workspaceList, "@") + "tmp").child("artifactory");
                File tempDirFile = new File(tempDir.getRemote());
                tempDirFile.mkdirs();
                tempDirFile.deleteOnExit();
                return tempDir;
            }
        });
    }
}
