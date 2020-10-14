package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
import org.jfrog.hudson.util.ExtractorUtils;

public class DockerPullExecutor extends BuildInfoProcessRunner {
    private ArtifactoryServer server;
    private String imageTag;
    private String host;
    private String targetRepo;


    public DockerPullExecutor(ArtifactoryServer pipelineServer, BuildInfo buildInfo, Run build, String imageTag, String targetRepo, String host, String javaArgs, Launcher launcher, TaskListener listener, FilePath ws, EnvVars envVars) {
        super(buildInfo, launcher, javaArgs, ws, "", "", envVars, listener, build);
        this.targetRepo = targetRepo;
        this.server = pipelineServer;
        this.imageTag = imageTag;
        this.host = host;
        // Remove trailing slash from target repo if needed.
        if (this.targetRepo != null && this.targetRepo.length() > 0 && this.targetRepo.endsWith("/")) {
            this.targetRepo = this.targetRepo.substring(0, this.targetRepo.length() - 1);
        }
    }

    public void execute() throws Exception {
        if (server == null) {
            throw new IllegalStateException("Artifactory server must be configured");
        }
        CommonResolver resolver = new CommonResolver();
        resolver.setServer(this.server);
        resolver.setRepo(targetRepo);
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new DockerEnvExtractor(build, buildInfo, null, resolver, listener, launcher, tempDir, env, imageTag, host);
        super.execute("docker", "org.jfrog.build.extractor.docker.extractor.DockerPull", envExtractor, tempDir);
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
