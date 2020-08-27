package org.jfrog.hudson.pipeline.common.executors;

import hudson.AbortException;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.jfrog.hudson.util.plugins.PluginsUtils;

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
            stepContext.onFailure(new AbortException("Artifactory server name is mandatory"));
        }

        List<ArtifactoryServer> artifactoryServers = new ArrayList<>();
        List<ArtifactoryServer> artifactoryConfiguredServers = RepositoriesUtils.getArtifactoryServers();
        if (artifactoryConfiguredServers == null) {
            stepContext.onFailure(new AbortException("No Artifactory servers were configured"));
            return;
        }
        for (ArtifactoryServer server : artifactoryConfiguredServers) {
            if (server.getServerId().equals(artifactoryServerID)) {
                artifactoryServers.add(server);
            }
        }
        if (artifactoryServers.isEmpty()) {
            stepContext.onFailure(new AbortException("Couldn't find Artifactory server with ID: " + artifactoryServerID));
        }
        if (artifactoryServers.size() > 1) {
            throw new RuntimeException("Duplicate configured Artifactory server ID: " + artifactoryServerID);
        }
        ArtifactoryServer server = artifactoryServers.get(0);
        artifactoryServer = new org.jfrog.hudson.pipeline.common.types.ArtifactoryServer(artifactoryServerID, server.getArtifactoryUrl(),
                server.getDeploymentThreads());
        if (PluginsUtils.isCredentialsPluginEnabled()) {
            artifactoryServer.setCredentialsId(server.getResolvingCredentialsConfig().getCredentialsId());
        } else {
            Credentials serverCredentials = server.getResolvingCredentialsConfig().provideCredentials(build.getParent());
            artifactoryServer.setUsername(serverCredentials.getUsername());
            artifactoryServer.setPassword(serverCredentials.getPassword());
        }
        artifactoryServer.setBypassProxy(server.isBypassProxy());
        artifactoryServer.getConnection().setRetry(server.getConnectionRetry());
        artifactoryServer.getConnection().setTimeout(server.getTimeout());
    }
}
