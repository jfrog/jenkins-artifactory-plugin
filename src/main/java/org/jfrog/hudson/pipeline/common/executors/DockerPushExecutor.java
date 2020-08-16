package org.jfrog.hudson.pipeline.common.executors;

import com.google.common.collect.ArrayListMultimap;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.CommonDeployer;
import org.jfrog.hudson.util.ExtractorUtils;

public class DockerPushExecutor extends BuildInfoProcessRunner {

    private ArtifactoryServer server;
    private String imageTag;
    private String targetRepo;
    private String host;
    private ArrayListMultimap<String, String> properties;

    public DockerPushExecutor(ArtifactoryServer pipelineServer, BuildInfo buildInfo, Run build, String imageTag, String targetRepo, String host, String javaArgs, Launcher launcher, ArrayListMultimap<String, String> properties, TaskListener listener, FilePath ws, EnvVars envVars) {
        super(buildInfo, launcher, javaArgs, ws, "", "", envVars, listener, build);
        this.server = pipelineServer;
        this.imageTag = imageTag;
        this.targetRepo = targetRepo;
        this.host = host;
        this.properties = properties;
        // Remove trailing slash from target repo if needed.
        if (this.targetRepo != null && this.targetRepo.length() > 0 && this.targetRepo.endsWith("/")) {
            this.targetRepo = this.targetRepo.substring(0, this.targetRepo.length() - 1);
        }
    }

    public void execute() throws Exception {
        if (server == null || targetRepo == null) {
            throw new IllegalStateException("Artifactory server & target repo must be configured");
        }
        CommonDeployer deployer = new CommonDeployer();
        deployer.setRepo(this.targetRepo);
        deployer.setServer(this.server);
        deployer.setProperties(this.properties);
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new DockerEnvExtractor(build, buildInfo, deployer, listener, launcher, tempDir, env, imageTag, host);
        super.execute("docker", "org.jfrog.build.extractor.docker.extractor.DockerPush", envExtractor, tempDir);
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
