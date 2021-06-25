package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;

public class CreateDockerBuildEnvExtractor extends EnvExtractor {
    private final String kanikoImageFile;
    private final String jibImageFiles;

    public CreateDockerBuildEnvExtractor(Run<?, ?> build, BuildInfo buildInfo, Deployer deployer,
                                         TaskListener buildListener, Launcher launcher, FilePath tempDir,
                                         EnvVars env, String kanikoImageFile, String jibImageFiles) {
        super(build, buildInfo, deployer, null, buildListener, launcher, tempDir, env);
        this.kanikoImageFile = kanikoImageFile;
        this.jibImageFiles = jibImageFiles;
    }

    @Override
    protected void addExtraConfiguration(ArtifactoryClientConfiguration configuration) {
        configuration.dockerHandler.setKanikoImageFile(kanikoImageFile);
        configuration.dockerHandler.setJibImageFile(jibImageFiles);
    }
}
