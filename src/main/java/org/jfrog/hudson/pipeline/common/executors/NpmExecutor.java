package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;
import org.jfrog.hudson.util.ExtractorUtils;

/**
 * @author yahavi
 */
public abstract class NpmExecutor extends BuildInfoProcessRunner {

    NpmBuild npmBuild;

    public NpmExecutor(BuildInfo buildInfo, Launcher launcher, NpmBuild npmBuild, String javaArgs, FilePath ws, String path, String module, EnvVars env, TaskListener listener, Run build) {
        super(buildInfo, launcher, javaArgs, ws, path, module, env, listener, build);
        this.npmBuild = npmBuild;
    }

    public void execute(Deployer deployer, Resolver resolver, String args, String classToExecute) throws Exception {
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new NpmEnvExtractor(build,
                buildInfo, deployer, resolver, listener, launcher, tempDir, env, args, path, module);
        super.execute("npm", "org.jfrog.build.extractor.npm.extractor." + classToExecute, envExtractor, tempDir);
    }
}
