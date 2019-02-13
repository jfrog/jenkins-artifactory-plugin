package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import org.acegisecurity.acls.NotFoundException;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.util.RepositoriesUtils;

import java.util.ArrayList;
import java.util.List;

public class GetArtifactoryServerExecutor implements Executor {

    private Run build;
    private StepContext stepContext;
    private String artifactoryServerID;
    private org.jfrog.hudson.pipeline.common.types.ArtifactoryServer artifactoryServer;

    public GetArtifactoryServerExecutor(Run build, StepContext stepContext, String artifactoryServerID) {
        this.build = build;
        this.stepContext = stepContext;
        this.artifactoryServerID = artifactoryServerID;
    }

    public org.jfrog.hudson.pipeline.common.types.ArtifactoryServer getArtifactoryServer() {
        return artifactoryServer;
    }

    @Override
    public void execute() {
        if (StringUtils.isEmpty(artifactoryServerID)) {
            stepContext.onFailure(new MissingArgumentException("Artifactory server name is mandatory"));
        }

        List<ArtifactoryServer> artifactoryServers = new ArrayList<>();
        List<ArtifactoryServer> artifactoryConfiguredServers = RepositoriesUtils.getArtifactoryServers();
        if (artifactoryConfiguredServers == null) {
            stepContext.onFailure(new NotFoundException("No Artifactory servers were configured"));
            return;
        }
        for (ArtifactoryServer server : artifactoryConfiguredServers) {
            if (server.getName().equals(artifactoryServerID)) {
                artifactoryServers.add(server);
            }
        }
        if (artifactoryServers.isEmpty()) {
            stepContext.onFailure(new NotFoundException("Couldn't find Artifactory server with ID: " + artifactoryServerID));
        }
        if (artifactoryServers.size() > 1) {
            throw new RuntimeException("Duplicate configured Artifactory server ID: " + artifactoryServerID);
        }
        ArtifactoryServer server = artifactoryServers.get(0);
        artifactoryServer = new org.jfrog.hudson.pipeline.common.types.ArtifactoryServer(artifactoryServerID, server.getUrl(),
                server.getResolvingCredentialsConfig().provideUsername(build.getParent()), server.getResolvingCredentialsConfig().providePassword(build.getParent()), server.getDeploymentThreads());
        artifactoryServer.setBypassProxy(server.isBypassProxy());
        artifactoryServer.getConnection().setRetry(server.getConnectionRetry());
        artifactoryServer.getConnection().setTimeout(server.getTimeout());
    }
}
