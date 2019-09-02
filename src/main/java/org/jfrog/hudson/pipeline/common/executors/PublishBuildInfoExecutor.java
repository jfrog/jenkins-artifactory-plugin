package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

public class PublishBuildInfoExecutor implements Executor {

    private ArtifactoryServer pipelineServer;
    private TaskListener listener;
    private BuildInfo buildInfo;
    private Run build;
    private FilePath ws;

    public PublishBuildInfoExecutor(Run build, TaskListener listener, BuildInfo buildInfo, ArtifactoryServer pipelineServer, FilePath ws) {
        this.pipelineServer = pipelineServer;
        this.buildInfo = buildInfo;
        this.listener = listener;
        this.build = build;
        this.ws = ws;
    }

    @Override
    public void execute() throws Exception {
        BuildInfoAccessor buildInfoAccessor = new BuildInfoAccessor(buildInfo);
        buildInfoAccessor.appendVcs(Utils.extractVcs(ws, new JenkinsBuildInfoLog(listener)));
        org.jfrog.hudson.ArtifactoryServer server = Utils.prepareArtifactoryServer(null, pipelineServer);
        try (ArtifactoryBuildInfoClient client = buildInfoAccessor.createArtifactoryClient(server, build, listener)) {
            buildInfoAccessor.createDeployer(build, listener, server, client).deploy();
        }
    }
}
