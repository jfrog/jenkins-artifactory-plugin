package org.jfrog.hudson.pipeline.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.DistributionConfig;
import org.jfrog.hudson.pipeline.common.types.PromotionConfig;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

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
        org.jfrog.hudson.ArtifactoryServer server = RepositoriesUtils.getArtifactoryServer(artifactoryServerID, RepositoriesUtils.getArtifactoryServers());
        if (server == null) {
            return null;
        }
        return server;
    }

    public static BuildInfo prepareBuildinfo(Run build, BuildInfo buildinfo) {
        if (buildinfo == null) {
            return new BuildInfo(build);
        }
        return buildinfo;
    }

    public static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
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

    public static String extractVcsRevision(FilePath filePath) throws IOException, InterruptedException {
        if (filePath == null) {
            return "";
        }
        FilePath dotGitPath = new FilePath(filePath, ".git");
        if (dotGitPath.exists()) {
            return dotGitPath.act(new MasterToSlaveFileCallable<String>() {
                public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                    FileRepository repository = new FileRepository(f);
                    ObjectId head = repository.resolve(Constants.HEAD);
                    return head.getName();
                }
            });
        }
        return extractVcsRevision(filePath.getParent());
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

    public static void exeConan(ArgumentListBuilder args, FilePath pwd, Launcher launcher, TaskListener listener, Run build, EnvVars env) {
        boolean failed;
        try {
            if (!pwd.exists()) {
                pwd.mkdirs();
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
            int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener).stderr(listener.getLogger()).pwd(pwd).join();
            failed = (exitValue != 0);
        } catch (Exception e) {
            listener.error("Couldn't execute the conan client executable. " + e.getMessage());
            build.setResult(Result.FAILURE);
            throw new Run.RunnerAbortedException();
        }
        if (failed) {
            build.setResult(Result.FAILURE);
            throw new Run.RunnerAbortedException();
        }
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
        List<String> mandatoryParams = new ArrayList<String>(Arrays.asList(ArtifactoryServer.BUILD_NAME, ArtifactoryServer.BUILD_NUMBER));
        List<String> allowedParams = Arrays.asList(ArtifactoryServer.BUILD_NAME, ArtifactoryServer.BUILD_NUMBER, targetRepository, "sourceRepo", "status", "comment", "includeDependencies", "copy", "failFast");

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
        List<String> mandatoryParams = new ArrayList<String>(Arrays.asList(ArtifactoryServer.BUILD_NAME, ArtifactoryServer.BUILD_NUMBER, "targetRepo"));
        List<String> allowedParams = Arrays.asList(ArtifactoryServer.BUILD_NAME, ArtifactoryServer.BUILD_NUMBER, "publish", "overrideExistingFiles", "gpgPassphrase", "async", "targetRepo", "sourceRepos", "dryRun");

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
    public static BuildInfo appendBuildInfo(CpsScript cpsScript, Map<String, Object> stepVariables) {
        BuildInfo buildInfo = (BuildInfo) stepVariables.get(BUILD_INFO);
        if (buildInfo == null) {
            buildInfo = (BuildInfo) cpsScript.invokeMethod("newBuildInfo", Maps.newLinkedHashMap());
            stepVariables.put(BUILD_INFO, buildInfo);
        }
        buildInfo.setCpsScript(cpsScript);
        return buildInfo;
    }

    public static ProxyConfiguration getProxyConfiguration(org.jfrog.hudson.ArtifactoryServer server) {
        if (server.isBypassProxy()) {
            return null;
        }
        return org.jfrog.hudson.ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy);
    }

    public static ArrayListMultimap<String, String> getPropertiesMap(BuildInfo buildInfo, Run build, StepContext context) throws IOException, InterruptedException {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();

        if (buildInfo.getName() != null) {
            properties.put("build.name", buildInfo.getName());
        } else {
            properties.put("build.name", BuildUniqueIdentifierHelper.getBuildName(build));
        }
        if (buildInfo.getNumber() != null) {
            properties.put("build.number", buildInfo.getNumber());
        } else {
            properties.put("build.number", BuildUniqueIdentifierHelper.getBuildNumber(build));
        }
        properties.put("build.timestamp", build.getTimestamp().getTime().getTime() + "");
        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            properties.put("build.parentName", ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject()));
            properties.put("build.parentNumber", parent.getUpstreamBuild() + "");
        }
        EnvVars env = context.get(EnvVars.class);
        String revision = ExtractorUtils.getVcsRevision(env);
        if (StringUtils.isNotBlank(revision)) {
            properties.put(BuildInfoFields.VCS_REVISION, revision);
        }
        return properties;
    }

    public static String replaceTildeWithUserHome(String path) {
        return path.replaceFirst("^~", System.getProperty("user.home"));
    }

    public static String getNpmExe(FilePath ws, TaskListener listener, EnvVars env, Launcher launcher, String nodeTool) throws IOException, InterruptedException {
        Log logger = new JenkinsBuildInfoLog(listener);
        String npmPath = "";
        String nodejsHome;
        // npm from tool
        if (StringUtils.isNotEmpty(nodeTool)) {
            npmPath = getNpmFromTool(ws, logger, listener, env, launcher, nodeTool);
        } else if ((nodejsHome = env.get("NODEJS_HOME")) != null) {
            // npm from environment
            npmPath = ws.child(nodejsHome).child("bin").child("npm").getRemote();
        }
        logger.debug("Using npm executable from " + StringUtils.defaultIfEmpty(npmPath, "PATH"));
        // If npmPath is empty, try to use npm from PATH
        return npmPath;
    }

    private static String getNpmFromTool(FilePath ws, Log logger, TaskListener listener, EnvVars env, Launcher launcher, String nodeTool) throws IOException, InterruptedException {
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
        FilePath nodePath = ws.child(nodeJsHome).child("bin").child("node");
        if (!nodePath.exists()) {
            logger.error("Couldn't find node executable in path " + nodePath.getRemote());
            throw new Run.RunnerAbortedException();
        }
        // Prepend NODEJS_HOME/bin to PATH
        env.override(NodeJSConstants.ENVVAR_NODEJS_PATH, nodePath.getParent().getRemote());
        return nodePath.sibling("npm").getRemote();
    }

    private static NodeJSInstallation getNpmInstallation(String nodeTool) {
        NodeJSInstallation[] installations = Jenkins.getInstance().getDescriptorByType(NodeJSInstallation.DescriptorImpl.class).getInstallations();
        return Arrays.stream(installations)
                .filter(i -> nodeTool.equals(i.getName()))
                .findFirst()
                .orElse(null);
    }
}