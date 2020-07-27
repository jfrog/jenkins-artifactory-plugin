package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;

/**
 * Created by Bar Belity on 07/07/2020.
 */
public class PipEnvExtractor extends EnvExtractor {

    private String args;
    private String path;
    private String module;
    private String envActivation;

    public PipEnvExtractor(Run build, BuildInfo buildInfo, Resolver resolver,
                           TaskListener buildListener, Launcher launcher, FilePath tempDir,
                           EnvVars env, String args, String path, String envActivation, String module) {
        super(build, buildInfo, null, resolver, buildListener, launcher, tempDir, env);
        this.args = args;
        this.path = path;
        this.module = module;
        this.envActivation = envActivation;
    }

    @Override
    protected void addExtraConfiguration(ArtifactoryClientConfiguration configuration) {
        configuration.buildToolHandler.setBuildToolArgs(args);
        configuration.buildToolHandler.setBuildToolPath(path);
        configuration.buildToolHandler.setBuildToolModule(module);
        configuration.pipHandler.setEnvActivation(envActivation);
    }
}
