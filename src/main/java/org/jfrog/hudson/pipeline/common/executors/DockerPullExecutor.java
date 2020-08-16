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

    public DockerPullExecutor(ArtifactoryServer pipelineServer, BuildInfo buildInfo, Run build, String imageTag, String host, String javaArgs, Launcher launcher, TaskListener listener, FilePath ws, EnvVars envVars) {
        super(buildInfo, launcher, javaArgs, ws, "", "", envVars, listener, build);

        this.server = pipelineServer;
        this.imageTag = imageTag;
        this.host = host;
    }

    public void execute() throws Exception {
        if (server == null) {
            throw new IllegalStateException("Artifactory server must be configured");
        }
        CommonResolver resolver = new CommonResolver();
        resolver.setServer(this.server);
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new DockerEnvExtractor(build, buildInfo, null, listener, launcher, tempDir, env, imageTag, host);
        super.execute("docker", "org.jfrog.build.extractor.docker.extractor.DockerPull", envExtractor, tempDir);
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
