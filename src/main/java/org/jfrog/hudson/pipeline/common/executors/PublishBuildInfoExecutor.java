package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;

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
        buildInfo.filterVariables();
        buildInfo.appendVcs(Utils.extractVcs(ws, new JenkinsBuildInfoLog(listener)));
        org.jfrog.hudson.ArtifactoryServer server = Utils.prepareArtifactoryServer(null, pipelineServer);
        try (ArtifactoryBuildInfoClient client = this.createArtifactoryClient(server, build, listener)) {
            buildInfo.createDeployer(build, listener, new ArtifactoryConfigurator(server), client).deploy();
        }
    }

    private ArtifactoryBuildInfoClient createArtifactoryClient(org.jfrog.hudson.ArtifactoryServer server, Run build, TaskListener listener) {
        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(new ArtifactoryConfigurator(server), server);
        return server.createArtifactoryClient(preferredDeployer.provideCredentials(build.getParent()),
                ProxyUtils.createProxyConfiguration(), new JenkinsBuildInfoLog(listener));
    }
}
