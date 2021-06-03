package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.CommonDeployer;
import org.jfrog.hudson.util.ExtractorUtils;

public class BuildDockerCreateExecutor extends BuildInfoProcessRunner {
    private final ArtifactoryServer server;
    private final String kanikoImageFile;
    private final String jibImageFile;
    private final String sourceRepo;

    public BuildDockerCreateExecutor(ArtifactoryServer pipelineServer, BuildInfo buildInfo, Run<?, ?> build,
                                     String kanikoImageFile, String jibImageFile, String sourceRepo, String javaArgs,
                                     Launcher launcher, TaskListener listener, FilePath ws, EnvVars envVars) {
        super(buildInfo, launcher, javaArgs, ws, "", "", envVars, listener, build);
        this.kanikoImageFile = kanikoImageFile;
        this.jibImageFile = jibImageFile;
        this.sourceRepo = sourceRepo;
        this.server = pipelineServer;
    }

    public void execute() throws Exception {
        if (server == null) {
            throw new IllegalStateException("Artifactory server must be configured");
        }
        if (StringUtils.isAllBlank(kanikoImageFile, jibImageFile) || StringUtils.isNoneBlank(kanikoImageFile, jibImageFile)) {
            throw new IllegalStateException("Exactly one of 'kanikoImageFile' or 'jibImageFile' must be configured");
        }
        CommonDeployer deployer = new CommonDeployer();
        deployer.setServer(server);
        deployer.setRepo(sourceRepo);
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new BuildDockerCreateEnvExtractor(build, buildInfo, deployer, listener, launcher, tempDir, env, kanikoImageFile, jibImageFile);
        super.execute("BuildDockerCreate", "org.jfrog.build.extractor.docker.extractor.BuildDockerCreate", envExtractor, tempDir);
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
