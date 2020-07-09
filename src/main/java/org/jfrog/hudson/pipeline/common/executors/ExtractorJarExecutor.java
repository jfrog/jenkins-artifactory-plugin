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

import java.util.Objects;

/**
 * Created by Bar Belity on 08/07/2020.
 */
public abstract class ExtractorJarExecutor implements Executor {

    TaskListener listener;
    BuildInfo buildInfo;
    Launcher launcher;
    String javaArgs;
    FilePath ws;
    String path;
    String module;
    EnvVars env;
    Run build;

    public ExtractorJarExecutor(BuildInfo buildInfo, Launcher launcher, String javaArgs, FilePath ws, String path, String module, EnvVars env, TaskListener listener, Run build) {
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
        Utils.launch(taskName, launcher, getArgs(absoluteDependencyDirPath, classToExecute), env, listener, ws);
        String generatedBuildPath = env.get(BuildInfoFields.GENERATED_BUILD_INFO);
        buildInfo.append(Utils.getGeneratedBuildInfo(build, listener, launcher, generatedBuildPath));
        buildInfo.setAgentName(Utils.getAgentName(ws));
    }

    private ArgumentListBuilder getArgs(String absoluteDependencyDirPath, String classToExecute) {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(Utils.getJavaPathBuilder(env.get("PATH+JDK"), launcher));
        if (StringUtils.isNotBlank(javaArgs)) {
            args.add(javaArgs.split("\\s+"));
        }
        args.add("-cp", absoluteDependencyDirPath + "/*");
        args.add(classToExecute);
        if (!launcher.isUnix()) {
            return args.toWindowsCommand();
        }
        return args;
    }
}
