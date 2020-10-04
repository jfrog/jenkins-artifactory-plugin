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

public class DockerEnvExtractor extends EnvExtractor {
    private String imageTag;
    private String host;

    public DockerEnvExtractor(Run build, BuildInfo buildInfo, Deployer deployer, Resolver resolver,
                              TaskListener buildListener, Launcher launcher, FilePath tempDir,
                              EnvVars env, String imageTag, String host) {
        super(build, buildInfo, deployer, resolver, buildListener, launcher, tempDir, env);
        this.imageTag = imageTag;
        this.host = host;
    }

    @Override
    protected void addExtraConfiguration(ArtifactoryClientConfiguration configuration) {
        configuration.dockerHandler.setHost(host);
        configuration.dockerHandler.setImageTag(imageTag);
    }
}
