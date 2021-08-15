package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;
import org.jfrog.hudson.util.ExtractorUtils;

public abstract class GoExecutor extends BuildInfoProcessRunner {

    GoBuild goBuild;

    public GoExecutor(BuildInfo buildInfo, Launcher launcher, GoBuild goBuild, String javaArgs, FilePath ws, String path, String module, EnvVars env, TaskListener listener, Run build) {
        super(buildInfo, launcher, javaArgs, ws, path, module, env, listener, build);
        this.goBuild = goBuild;
    }

    public void execute(Deployer deployer, Resolver resolver, String args, String version, String classToExecute) throws Exception {
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new GoEnvExtractor(build, buildInfo, deployer, resolver, listener, launcher,
                tempDir, env, args, version, path, module);
        super.execute("go", "org.jfrog.build.extractor.go.extractor." + classToExecute, envExtractor, tempDir);
    }
}
