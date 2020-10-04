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
        resolver.setRepo(getDockerRepo(this.imageTag));
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new DockerEnvExtractor(build, buildInfo, null, resolver, listener, launcher, tempDir, env, imageTag, host);
        super.execute("docker", "org.jfrog.build.extractor.docker.extractor.DockerPull", envExtractor, tempDir);
    }

    // In order to push/pull from images from Artifactory images must be present in following template:
    // artprod.mycompany/<DOCKER_REPOSITORY>:<DOCKER_TAG>
    // 'getDockerRepo' returns the DOCKER_REPOSITORY
    private String getDockerRepo(String imageTag) {
        int dockerRepoStartidx = imageTag.indexOf('/');
        int dockerRepoEndidx = imageTag.indexOf(':');
        if (dockerRepoStartidx == -1 || dockerRepoEndidx == -1) {
            return "";
        }
        return imageTag.substring(dockerRepoStartidx + 1, dockerRepoEndidx);
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
