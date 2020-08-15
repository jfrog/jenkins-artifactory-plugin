package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;

import java.util.HashMap;


public class DockerEnvExtractor extends EnvExtractor {
    private String imageTag;
    private String host;
    private HashMap<String, String> properties;

    public DockerEnvExtractor(Run build, BuildInfo buildInfo, Deployer deployer,
                              TaskListener buildListener, Launcher launcher, FilePath tempDir,
                              EnvVars env, String imageTag, String host, HashMap<String, String> properties) {
        super(build, buildInfo, deployer, null, buildListener, launcher, tempDir, env);
        this.imageTag = imageTag;
        this.host = host;
        this.properties = properties;
    }

    @Override
    protected void addExtraConfiguration(ArtifactoryClientConfiguration configuration) {
        configuration.dockerHandler.setHost(host);
        configuration.dockerHandler.setImageTag(imageTag);
        configuration.dockerHandler.setArtifactProperties(properties);
    }
}
