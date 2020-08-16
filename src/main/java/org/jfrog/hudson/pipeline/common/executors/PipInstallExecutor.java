package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.PipBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

/**
 * Created by Bar Belity on 07/07/2020.
 */
public class PipInstallExecutor extends BuildInfoProcessRunner {

    PipBuild pipBuild;
    String pipArgs;
    String envActivation;
    Log logger;

    public PipInstallExecutor(BuildInfo buildInfo, Launcher launcher, PipBuild pipBuild, String javaArgs, String pipArgs, FilePath ws, String envActivation, String module, EnvVars env, TaskListener listener, Run build) {
        super(buildInfo, launcher, javaArgs, ws, null, module, env, listener, build);
        this.pipBuild = pipBuild;
        this.pipArgs = pipArgs;
        this.envActivation = envActivation;
        this.logger = new JenkinsBuildInfoLog(listener);
    }

    @Override
    public void execute() throws Exception {
        CommonResolver resolver = (CommonResolver) pipBuild.getResolver();
        if (resolver.isEmpty()) {
            throw new IllegalStateException("Resolver must be configured with resolution repository and Artifactory server");
        }
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        module = StringUtils.isNotBlank(module) ? module : buildInfo.getName();
        EnvExtractor envExtractor = new PipEnvExtractor(build, buildInfo, resolver, listener, launcher, tempDir, env, pipArgs, path, envActivation, module);
        super.execute("pip", "org.jfrog.build.extractor.pip.extractor.PipInstall", envExtractor, tempDir);
    }
}
