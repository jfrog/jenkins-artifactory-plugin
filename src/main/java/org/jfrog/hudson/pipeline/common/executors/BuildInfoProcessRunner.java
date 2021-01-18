package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.PluginDependencyHelper;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by Bar Belity on 08/07/2020.
 * <p>
 * Base class for build-info external processes.
 * Used for running build-tools extractors in a new java process.
 */
public abstract class BuildInfoProcessRunner implements Executor {

    TaskListener listener;
    BuildInfo buildInfo;
    Launcher launcher;
    String javaArgs;
    FilePath ws;
    String path;
    String module;
    EnvVars env;
    Run build;

    public BuildInfoProcessRunner(BuildInfo buildInfo, Launcher launcher, String javaArgs, FilePath ws, String path, String module, EnvVars env, TaskListener listener, Run build) {
        this.listener = listener;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.launcher = launcher;
        this.javaArgs = javaArgs;
        this.ws = ws;
        this.path = Objects.toString(path, ".");
        this.module = module;
        this.env = env;
        this.build = build;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void execute(String taskName, String classToExecute, EnvExtractor envExtractor, FilePath tempDir) throws Exception {
        ExtractorUtils.addVcsDetailsToEnv(new FilePath(ws, path), env, listener);
        envExtractor.execute();
        String absoluteDependencyDirPath = PluginDependencyHelper.copyExtractorJars(env, tempDir);
        FilePath javaTmpDir = new FilePath(tempDir, "javatmpdir");
        try {
            Utils.launch(taskName, launcher, getArgs(absoluteDependencyDirPath, classToExecute, javaTmpDir), env, listener, ws);
        } finally {
            if (javaTmpDir.exists()) {
                javaTmpDir.deleteRecursive();
            }
        }
        String generatedBuildPath = env.get(BuildInfoFields.GENERATED_BUILD_INFO);
        buildInfo.append(Utils.getGeneratedBuildInfo(build, listener, launcher, generatedBuildPath));
        buildInfo.setAgentName(Utils.getAgentName(ws));
    }

    private ArgumentListBuilder getArgs(String absoluteDependencyDirPath, String classToExecute, FilePath javaTmpDir) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(Utils.getJavaPathBuilder(env.get("PATH+JDK"), launcher));
        if (StringUtils.isNotBlank(javaArgs)) {
            args.add(javaArgs.split("\\s+"));
        }
        if (args.toList().stream().noneMatch(s -> s.contains("java.io.tmpdir"))) {
            if (!javaTmpDir.exists()) {
                javaTmpDir.mkdirs();
            }
            args.add("-Djava.io.tmpdir=" + javaTmpDir.getRemote());
        }
        args.add("-cp", absoluteDependencyDirPath + "/*");
        args.add(classToExecute);
        if (!launcher.isUnix()) {
            return args.toWindowsCommand();
        }
        return args;
    }
}
