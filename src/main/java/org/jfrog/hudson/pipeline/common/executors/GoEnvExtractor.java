package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;

public class GoEnvExtractor extends EnvExtractor {
    private String args;
    private String path;
    private String module;
    private String version;

    public GoEnvExtractor(Run build, BuildInfo buildInfo, Deployer deployer, Resolver resolver,
                          TaskListener buildListener, Launcher launcher, FilePath tempDir,
                          EnvVars env, String args, String version, String path, String module) {
        super(build, buildInfo, deployer, resolver, buildListener, launcher, tempDir, env);
        this.args = args;
        this.path = path;
        this.module = module;
        this.version = version;
    }

    @Override
    protected void addExtraConfiguration(ArtifactoryClientConfiguration configuration) {
        configuration.packageManagerHandler.setArgs(args);
        configuration.packageManagerHandler.setPath(path);
        configuration.packageManagerHandler.setModule(module);
        configuration.goHandler.setGoPublishedVersion(version);
    }
}
