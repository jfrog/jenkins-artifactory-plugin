package org.jfrog.hudson.pipeline.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.git.util.BuildData;
import hudson.remoting.Channel;
import hudson.remoting.LocalChannel;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import jenkins.plugins.nodejs.NodeJSConstants;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.util.GitUtils;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.DistributionConfig;
import org.jfrog.hudson.pipeline.common.types.PromotionConfig;
import org.jfrog.hudson.pipeline.common.types.XrayScanConfig;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

import static org.jfrog.hudson.pipeline.common.types.ArtifactoryServer.BUILD_NAME;
import static org.jfrog.hudson.pipeline.common.types.ArtifactoryServer.BUILD_NUMBER;

/**
 * Created by romang on 4/24/16.
 */
public class Utils {

    public static final String CONAN_USER_HOME = "CONAN_USER_HOME"; // Conan user home environment variable name
    public static final String BUILD_INFO = "buildInfo"; // The build info argument used in pipeline
    private static final String UNIX_SPECIAL_CHARS = "`^<>| ,;!?'\"()[]{}$*\\&#"; // Unix special characters to escape in '/bin/sh' execution

    /**
     * Prepares Artifactory server either from serverID or from ArtifactoryServer.
     *
     * @param artifactoryServerID
     * @param pipelineServer
     * @return
     */
    public static org.jfrog.hudson.ArtifactoryServer prepareArtifactoryServer(String artifactoryServerID,
                                                                              ArtifactoryServer pipelineServer) {

        if (artifactoryServerID == null && pipelineServer == null) {
            return null;
        }
        if (artifactoryServerID != null && pipelineServer != null) {
            return null;
        }
        if (pipelineServer != null) {
            CredentialsConfig credentials = pipelineServer.createCredentialsConfig();

            return new org.jfrog.hudson.ArtifactoryServer(null, pipelineServer.getUrl(), credentials,
                    credentials, pipelineServer.getConnection().getTimeout(), pipelineServer.isBypassProxy(), pipelineServer.getConnection().getRetry(), pipelineServer.getDeploymentThreads());
        }
        return RepositoriesUtils.getArtifactoryServer(artifactoryServerID, RepositoriesUtils.getArtifactoryServers());
    }

    public static BuildInfo prepareBuildinfo(Run build, BuildInfo buildinfo) {
        if (buildinfo == null) {
            return new BuildInfo(build);
        }
        return buildinfo;
    }

    public static EnvVars extractBuildParameters(Run build, TaskListener listener) {
        EnvVars buildVariables = new EnvVars();
        try {
            ParametersAction parameters = build.getAction(ParametersAction.class);
            if (parameters != null) {
                for (ParameterValue p : parameters) {
                    if (p.isSensitive()) {
                        continue;
                    }
                    String v = p.createVariableResolver(null).resolve(p.getName());
                    if (v != null) {
                        buildVariables.put(p.getName(), v);
                    }
                }
            }
        } catch (Exception e) {
            listener.getLogger().println("Can't get build variables");
            return null;
        }

        return buildVariables;
    }

    public static List<Vcs> extractVcsBuildData(Run build) {
        List<Vcs> result = new ArrayList<Vcs>();
        List<BuildData> buildData = build.getActions(BuildData.class);
        if (buildData != null) {
            for (BuildData data : buildData) {
                String sha1 = data.getLastBuiltRevision().getSha1String();
                Iterator<String> iterator = data.getRemoteUrls().iterator();
                if (iterator.hasNext()) {
                    result.add(new Vcs(iterator.next(), sha1));
                }
            }
        }
        return result;
    }

    public static Vcs extractVcs(FilePath filePath, Log log) throws IOException, InterruptedException {
        return filePath.act(new MasterToSlaveFileCallable<Vcs>() {
            public Vcs invoke(File f, VirtualChannel channel) throws IOException {
                return GitUtils.extractVcs(f, log);
            }
        });
    }

    public static Computer getCurrentComputer(Launcher launcher) {
        Jenkins j = Jenkins.getInstance();
        for (Computer c : j.getComputers()) {
            if (c.getChannel() == launcher.getChannel()) {
                return c;
            }
        }
        return null;
    }

    public static IncludesExcludes getArtifactsIncludeExcludeForDeyployment(IncludeExcludePatterns patternFilter) {
        if (patternFilter == null) {
            return new IncludesExcludes("", "");
        }
        String[] excludePatterns = patternFilter.getExcludePatterns();
        String[] includePatterns = patternFilter.getIncludePatterns();
        StringBuilder include = new StringBuilder();
        StringBuilder exclude = new StringBuilder();
        for (int i = 0; i < includePatterns.length; i++) {
            if (include.length() > 0) {
                include.append(", ");
            }
            include.append(includePatterns[i]);
        }
        for (int i = 0; i < excludePatterns.length; i++) {
            if (exclude.length() > 0) {
                exclude.append(", ");
            }
            exclude.append(excludePatterns[i]);
        }

        IncludesExcludes result = new IncludesExcludes(include.toString(), exclude.toString());
        return result;
    }

    public static org.jfrog.build.api.Build getGeneratedBuildInfo(Run build, TaskListener listener, Launcher launcher, String jsonBuildPath) {
        ObjectMapper mapper = new ObjectMapper();
        FilePath generatedBuildInfoFilePath = null;
        InputStream inputStream = null;
        try {
            StringWriter writer = new StringWriter();
            generatedBuildInfoFilePath = new FilePath(launcher.getChannel(), jsonBuildPath);
            inputStream = generatedBuildInfoFilePath.read();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String buildInfoFileContent = writer.toString();
            if (StringUtils.isBlank(buildInfoFileContent)) {
                return new org.jfrog.build.api.Build();
            }
            return mapper.readValue(buildInfoFileContent, org.jfrog.build.api.Build.class);
        } catch (Exception e) {
            listener.error("Couldn't read generated build info at : " + jsonBuildPath);
            build.setResult(Result.FAILURE);
            throw new Run.RunnerAbortedException();
        } finally {
            if (inputStream != null) {
                IOUtils.closeQuietly(inputStream);
            }
            deleteFilePathQuietly(generatedBuildInfoFilePath);
        }
    }

    private static void deleteFilePathQuietly(FilePath filePath) {
        try {
            if (filePath != null && filePath.exists()) {
                filePath.delete();
            }
        } catch (Exception e) {
            // Ignore exceptions
        }
    }

    public static String createTempJsonFile(Launcher launcher, final String name, final String dir) throws Exception {
        return launcher.getChannel().call(new MasterToSlaveCallable<String, Exception>() {
            public String call() throws IOException {
                File tempFile = File.createTempFile(name, ".json", new File(dir));
                tempFile.deleteOnExit();
                return tempFile.getAbsolutePath();
            }
        });
    }

    public static void exeConan(ArgumentListBuilder args, FilePath ws, Launcher launcher, TaskListener listener, EnvVars env) {
        try {
            if (!ws.exists()) {
                ws.mkdirs();
            }
            if (launcher.isUnix()) {
                boolean hasMaskedArguments = args.hasMaskedArguments();
                StringBuilder sb = new StringBuilder();
                for (String arg : args.toList()) {
                    sb.append(escapeUnixArgument(arg)).append(" ");
                }
                args.clear();
                args.add("sh", "-c");
                if (hasMaskedArguments) {
                    args.addMasked(sb.toString());
                } else {
                    args.add(sb.toString());
                }
            } else {
                args = args.toWindowsCommand();
            }
        } catch (Exception e) {
            listener.error("Couldn't execute the conan client executable. " + e.getMessage());
            throw new Run.RunnerAbortedException();
        }
        launch("Conan", launcher, args, env, listener, ws);
    }

    /**
     * Launch a process. Throw a RuntimeException in case of an error.
     *
     * @param taskName - The task name - Maven, Gradle, npm, etc.
     * @param launcher - The launcher
     * @param args     - The arguments
     * @param env      - Task environment
     * @param listener - Task listener
     * @param ws       - The workspace
     */
    public static void launch(String taskName, Launcher launcher, ArgumentListBuilder args, EnvVars env, TaskListener listener, FilePath ws) {
        boolean failed;
        try {
            int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener).stderr(listener.getLogger()).pwd(ws).join();
            failed = (exitValue != 0);
        } catch (Exception e) {
            listener.error("Couldn't execute " + taskName + " task. " + ExceptionUtils.getMessage(e));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException(taskName + " build failed");
        }
    }

    public static String getJavaPathBuilder(String jdkBinPath, Launcher launcher) {
        StringBuilder javaPathBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(jdkBinPath)) {
            javaPathBuilder.append(jdkBinPath).append("/");
        }
        javaPathBuilder.append("java");
        if (!launcher.isUnix()) {
            javaPathBuilder.append(".exe");
        }
        return javaPathBuilder.toString();
    }

    public static String escapeUnixArgument(String arg) {
        StringBuilder res = new StringBuilder();
        for (char c : arg.toCharArray()) {
            if (UNIX_SPECIAL_CHARS.indexOf(c) >= 0) {
                res.append("\\");
            }
            res.append(c);
        }
        return res.toString();
    }

    public static PromotionConfig createPromotionConfig(Map<String, Object> promotionParams, boolean isTargetRepositoryMandatory) {
        final String targetRepository = "targetRepo";
        List<String> mandatoryParams = new ArrayList<>(Arrays.asList(BUILD_NAME, BUILD_NUMBER));
        List<String> allowedParams = Arrays.asList(BUILD_NAME, BUILD_NUMBER, targetRepository, "sourceRepo", "status", "comment", "includeDependencies", "copy", "failFast");

        if (isTargetRepositoryMandatory) {
            mandatoryParams.add(targetRepository);
        }
        if (!promotionParams.keySet().containsAll(mandatoryParams)) {
            throw new IllegalArgumentException(mandatoryParams.toString() + " are mandatory arguments");
        }
        if (!allowedParams.containsAll(promotionParams.keySet())) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + allowedParams.toString());
        }
        final ObjectMapper mapper = new ObjectMapper();
        PromotionConfig config = mapper.convertValue(promotionParams, PromotionConfig.class);

        return config;
    }

    public static org.jfrog.hudson.release.promotion.PromotionConfig convertPromotionConfig(PromotionConfig pipelinePromotionConfig) {
        org.jfrog.hudson.release.promotion.PromotionConfig promotionConfig = new org.jfrog.hudson.release.promotion.PromotionConfig();
        promotionConfig.setBuildName(pipelinePromotionConfig.getBuildName());
        promotionConfig.setBuildNumber(pipelinePromotionConfig.getBuildNumber());
        promotionConfig.setTargetRepo(pipelinePromotionConfig.getTargetRepo());
        promotionConfig.setSourceRepo(pipelinePromotionConfig.getSourceRepo());
        promotionConfig.setStatus(pipelinePromotionConfig.getStatus());
        promotionConfig.setComment(pipelinePromotionConfig.getComment());
        promotionConfig.setIncludeDependencies(pipelinePromotionConfig.isIncludeDependencies());
        promotionConfig.setCopy(pipelinePromotionConfig.isCopy());
        promotionConfig.setFailFast(pipelinePromotionConfig.isFailFast());
        return promotionConfig;
    }

    public static DistributionConfig createDistributionConfig(Map<String, Object> promotionParams) {
        List<String> mandatoryParams = new ArrayList<>(Arrays.asList(BUILD_NAME, BUILD_NUMBER, "targetRepo"));
        List<String> allowedParams = Arrays.asList(BUILD_NAME, BUILD_NUMBER, "publish", "overrideExistingFiles", "gpgPassphrase", "async", "targetRepo", "sourceRepos", "dryRun");

        if (!promotionParams.keySet().containsAll(mandatoryParams)) {
            throw new IllegalArgumentException(mandatoryParams.toString() + " are mandatory arguments");
        }
        if (!allowedParams.containsAll(promotionParams.keySet())) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + allowedParams.toString());
        }
        final ObjectMapper mapper = new ObjectMapper();
        DistributionConfig config = mapper.convertValue(promotionParams, DistributionConfig.class);

        return config;
    }

    public static String getAgentName(FilePath ws) {
        if (ws.getChannel() != null) {
            return ws.getChannel() instanceof LocalChannel ? "Master" : ((Channel) ws.getChannel()).getName();
        }
        return "Unknown";
    }

    /**
     * Add the buildInfo to step variables if missing and set its cps script.
     *
     * @param cpsScript     the cps script
     * @param stepVariables step variables map
     * @return the build info
     */
    public static void appendBuildInfo(CpsScript cpsScript, Map<String, Object> stepVariables) {
        BuildInfo buildInfo = (BuildInfo) stepVariables.get(BUILD_INFO);
        if (buildInfo == null) {
            buildInfo = (BuildInfo) cpsScript.invokeMethod("newBuildInfo", Maps.newLinkedHashMap());
            stepVariables.put(BUILD_INFO, buildInfo);
        }
        buildInfo.setCpsScript(cpsScript);
    }

    public static ProxyConfiguration getProxyConfiguration(org.jfrog.hudson.ArtifactoryServer server) {
        if (server.isBypassProxy()) {
            return null;
        }
        return ProxyUtils.createProxyConfiguration();
    }

    public static ArrayListMultimap<String, String> getPropertiesMap(BuildInfo buildInfo, Run build, StepContext context) throws IOException, InterruptedException {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();

        if (buildInfo.getName() != null) {
            properties.put(BuildInfoFields.BUILD_NAME, buildInfo.getName());
        } else {
            properties.put(BuildInfoFields.BUILD_NAME, BuildUniqueIdentifierHelper.getBuildName(build));
        }
        if (buildInfo.getNumber() != null) {
            properties.put(BuildInfoFields.BUILD_NUMBER, buildInfo.getNumber());
        } else {
            properties.put(BuildInfoFields.BUILD_NUMBER, BuildUniqueIdentifierHelper.getBuildNumber(build));
        }
        properties.put(BuildInfoFields.BUILD_TIMESTAMP, build.getTimestamp().getTime().getTime() + "");
        addParentBuildProps(properties, build);
        EnvVars env = context.get(EnvVars.class);
        addVcsDetailsToProps(env, properties);
        return properties;
    }

    public static void addParentBuildProps(ArrayListMultimap<String, String> properties, Run build) {
        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            properties.put(BuildInfoFields.BUILD_PARENT_NAME, ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject()));
            properties.put(BuildInfoFields.BUILD_PARENT_NUMBER, parent.getUpstreamBuild() + "");
        }
    }

    public static void addVcsDetailsToProps(EnvVars env, ArrayListMultimap<String, String> properties) {
        String revision = ExtractorUtils.getVcsRevision(env);
        if (StringUtils.isNotBlank(revision)) {
            properties.put(BuildInfoFields.VCS_REVISION, revision);
        }
        String gitUrl = ExtractorUtils.getVcsUrl(env);
        if (StringUtils.isNotBlank(gitUrl)) {
            properties.put(BuildInfoFields.VCS_URL, gitUrl);
        }
    }

    public static String replaceTildeWithUserHome(String path) {
        return path.replaceFirst("^~", System.getProperty("user.home"));
    }

    /**
     * Add possible npm executable directories to PATH environment variable.
     *
     * @param ws       - Current workspace
     * @param listener - The listener
     * @param env      - Agent's environment variables
     * @param launcher - The Launcher
     * @param nodeTool - NodeJS tool, if requested
     */
    public static void addNpmToPath(FilePath ws, TaskListener listener, EnvVars env, Launcher launcher, String nodeTool) throws IOException, InterruptedException {
        String nodejsHome;
        // npm from tool
        if (StringUtils.isNotEmpty(nodeTool)) {
            prependNpmToPathFromTool(ws, listener, env, launcher, nodeTool);
        } else if ((nodejsHome = env.get("NODEJS_HOME")) != null) {
            prependNodeJSHomeToPath(env, ws.child(nodejsHome));
        }
    }

    /**
     * Prepend npm path from NodeJS tool, as used in the NodeJS plugin.
     *
     * @param ws       - Current workspace
     * @param listener - The listener
     * @param env      - Agent's environment variables
     * @param launcher - The Launcher
     * @param nodeTool - NodeJS tool, if requested
     */
    private static void prependNpmToPathFromTool(FilePath ws, TaskListener listener, EnvVars env, Launcher launcher, String nodeTool) throws IOException, InterruptedException {
        Log logger = new JenkinsBuildInfoLog(listener);
        NodeJSInstallation nodeInstallation = getNpmInstallation(nodeTool);
        if (nodeInstallation == null) {
            logger.error("Couldn't find NodeJS tool '" + nodeTool + "'");
            throw new Run.RunnerAbortedException();
        }
        Node node = ActionableHelper.getNode(launcher);
        String nodeJsHome = nodeInstallation.forNode(node, listener).forEnvironment(env).getHome();
        if (StringUtils.isBlank(nodeJsHome)) {
            logger.error("Couldn't find NodeJS home");
            throw new Run.RunnerAbortedException();
        }
        prependNodeJSHomeToPath(env, ws.child(nodeJsHome));
    }

    /**
     * Prepend npm path from NODEJS_HOME environment variable.
     *
     * @param env        - Agent's environment variables
     * @param nodeJsHome - Path to NodeJS home
     */
    private static void prependNodeJSHomeToPath(EnvVars env, FilePath nodeJsHome) {
        // For Linux/Unix - Prepend NODEJS_HOME/bin to PATH
        env.override(NodeJSConstants.ENVVAR_NODEJS_PATH, nodeJsHome.child("bin").getRemote());
        // For Windows - Prepend NODEJS_HOME to PATH
        env.override(NodeJSConstants.ENVVAR_NODEJS_PATH, nodeJsHome.getRemote());
    }

    private static NodeJSInstallation getNpmInstallation(String nodeTool) {
        NodeJSInstallation[] installations = Jenkins.getInstance().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class).getInstallations();
        return Arrays.stream(installations)
                .filter(i -> nodeTool.equals(i.getName()))
                .findFirst()
                .orElse(null);
    }

    public static XrayScanConfig createXrayScanConfig(Map<String, Object> xrayScanParams) {
        final String failBuild = "failBuild";

        List<String> mandatoryArgumentsAsList = Arrays.asList(BUILD_NAME, BUILD_NUMBER);
        if (!xrayScanParams.keySet().containsAll(mandatoryArgumentsAsList)) {
            throw new IllegalArgumentException(mandatoryArgumentsAsList.toString() + " are mandatory arguments");
        }

        Set<String> xrayScanParamsSet = xrayScanParams.keySet();
        List<String> keysAsList = Arrays.asList(BUILD_NAME, BUILD_NUMBER, failBuild);
        if (!keysAsList.containsAll(xrayScanParamsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        return new XrayScanConfig((String) xrayScanParams.get(BUILD_NAME),
                (String) xrayScanParams.get(BUILD_NUMBER), (Boolean) xrayScanParams.get(failBuild));
    }
}