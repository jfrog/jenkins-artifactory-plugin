package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;

public class PublishBuildInfoExecutor implements Executor {

    private ArtifactoryServer pipelineServer;
    private TaskListener listener;
    private BuildInfo buildInfo;
    private Run build;

    public PublishBuildInfoExecutor(Run build, TaskListener listener, BuildInfo buildInfo, ArtifactoryServer pipelineServer) {
        this.pipelineServer = pipelineServer;
        this.buildInfo = buildInfo;
        this.listener = listener;
        this.build = build;
    }

    @Override
    public void execute() throws Exception {
        BuildInfoAccessor buildInfoAccessor = new BuildInfoAccessor(buildInfo);
        org.jfrog.hudson.ArtifactoryServer server = Utils.prepareArtifactoryServer(null, pipelineServer);
        try (ArtifactoryBuildInfoClient client = buildInfoAccessor.createArtifactoryClient(server, build, listener)) {
            buildInfoAccessor.createDeployer(build, listener, server, client).deploy();
        }
    }
}
