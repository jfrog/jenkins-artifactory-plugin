package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.gradle.Gradle;
import hudson.plugins.gradle.GradleInstallation;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.gradle.GradleInitScriptWriter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GradleBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.util.ExtractorUtils;

import java.io.IOException;
import java.util.Objects;

public class GradleExecutor implements Executor {

    private EnvVars env, extendedEnv;
    private GradleBuild gradleBuild;
    private TaskListener listener;
    private FilePath tempDir, ws;
    private BuildInfo buildInfo;
    private Launcher launcher;
    private String initScriptPath;
    private String buildFile;
    private String switches;
    private String rootDir;
    private String tasks;
    private Run build;

    public GradleExecutor(Run build, GradleBuild gradleBuild, String tasks, String buildFile, String rootDir, String switches, BuildInfo buildInfo, EnvVars env, FilePath ws, TaskListener listener, Launcher launcher) {
        this.build = build;
        this.gradleBuild = gradleBuild;
        this.tasks = Objects.toString(tasks, "artifactoryPublish");
        this.buildFile = StringUtils.defaultIfEmpty(buildFile, "build.gradle");
        this.rootDir = Objects.toString(rootDir, "");
        this.switches = Objects.toString(switches, "");
        this.buildInfo = buildInfo;
        this.env = env;
        this.ws = ws;
        this.listener = listener;
        this.launcher = launcher;
    }

    @Override
    public void execute() throws Exception {
        buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        Deployer deployer = gradleBuild.getDeployer();
        deployer.createPublisherBuildInfoDetails(buildInfo);
        extendedEnv = new EnvVars(env);
        ExtractorUtils.addVcsDetailsToEnv(new FilePath(ws, rootDir), extendedEnv, listener);
        tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new MavenGradleEnvExtractor(build,
                buildInfo, deployer, gradleBuild.getResolver(), listener, launcher, tempDir, extendedEnv);
        envExtractor.execute();
        ArgumentListBuilder args = getGradleExecutor();
        Utils.launch("Gradle", launcher, args, extendedEnv, listener, ws);
        String generatedBuildPath = extendedEnv.get(BuildInfoFields.GENERATED_BUILD_INFO);
        buildInfo.append(Utils.getGeneratedBuildInfo(build, listener, launcher, generatedBuildPath));
        ActionableHelper.deleteFilePath(tempDir, initScriptPath);
        // Read the deployable artifacts map from the 'json' file in the agent and append them to the buildInfo object.
        buildInfo.getAndAppendDeployableArtifactsByModule(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS),
                extendedEnv.get(BuildInfoFields.BACKWARD_COMPATIBLE_DEPLOYABLE_ARTIFACTS), tempDir, listener);
        buildInfo.setAgentName(Utils.getAgentName(ws));
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    private ArgumentListBuilder getGradleExecutor() {
        ArgumentListBuilder args = new ArgumentListBuilder();
        if (gradleBuild.isUseWrapper()) {
            String execName = launcher.isUnix() ? GradleInstallation.UNIX_GRADLE_WRAPPER_COMMAND : GradleInstallation.WINDOWS_GRADLE_WRAPPER_COMMAND;
            FilePath gradleWrapperFile = new FilePath(new FilePath(ws, rootDir), execName);
            args.add(gradleWrapperFile.getRemote());
        } else {
            try {
                args.add(getGradleExe());
            } catch (Exception e) {
                listener.error("Couldn't find Gradle executable.");
                throw new Run.RunnerAbortedException();
            }
        }
        args.addTokenized(getSwitches()).
                addTokenized(tasks).
                add("-b", getBuildFileFullPath());
        if (!launcher.isUnix()) {
            args = args.toWindowsCommand();
        }
        return args;
    }

    private String getBuildFileFullPath() {
        StringBuilder buildFile = new StringBuilder();
        if (StringUtils.isNotEmpty(rootDir)) {
            String pathsDelimiter = launcher.isUnix() ? "/" : "\\";
            buildFile.append(rootDir);
            if (!StringUtils.endsWith(rootDir, pathsDelimiter)) {
                buildFile.append(pathsDelimiter);
            }
        }
        buildFile.append(this.buildFile);
        return buildFile.toString();
    }

    private String getSwitches() {
        String switches = this.switches;
        if (!gradleBuild.isUsesPlugin()) {
            try {
                initScriptPath = createInitScript();
                switches += " --init-script " + initScriptPath;
            } catch (Exception e) {
                listener.getLogger().println("Error occurred while writing Gradle Init Script: " + e.getMessage());
                throw new Run.RunnerAbortedException();
            }
        }
        return switches;
    }

    private GradleInstallation getGradleInstallation() {
        if (!StringUtils.isEmpty(gradleBuild.getTool())) {
            GradleInstallation[] installations = Jenkins.getInstance().getDescriptorByType(Gradle.DescriptorImpl.class).getInstallations();
            for (GradleInstallation i : installations) {
                if (gradleBuild.getTool().equals(i.getName())) {
                    return i;
                }
            }
        }
        return null;
    }

    private String getGradleExe() throws IOException, InterruptedException {
        if (StringUtils.isNotEmpty(gradleBuild.getTool())) {
            GradleInstallation gi = getGradleInstallation();
            if (gi == null) {
                listener.error("Couldn't find Gradle executable.");
                throw new Run.RunnerAbortedException();
            } else {
                Node node = ActionableHelper.getNode(launcher);
                gi = gi.forNode(node, listener);
                gi = gi.forEnvironment(extendedEnv);
            }

            extendedEnv.put("GRADLE_HOME", gi.getHome());
            String gradleExe = gi.getExecutable(launcher);
            if (gradleExe != null) {
                return gradleExe;
            }
        }
        if (!extendedEnv.containsKey("GRADLE_HOME")) {
            throw new RuntimeException("Couldn't find gradle installation");
        }
        return extendedEnv.get("GRADLE_HOME") + "/bin/gradle";
    }

    private String createInitScript() throws Exception {
        GradleInitScriptWriter writer = new GradleInitScriptWriter(tempDir);
        FilePath initScript = tempDir.createTextTempFile("init-artifactory", "gradle", writer.generateInitScript(env));
        ActionableHelper.deleteFilePathOnExit(initScript);
        String initScriptPath = initScript.getRemote();
        initScriptPath = initScriptPath.replace('\\', '/');
        return initScriptPath;
    }
}
