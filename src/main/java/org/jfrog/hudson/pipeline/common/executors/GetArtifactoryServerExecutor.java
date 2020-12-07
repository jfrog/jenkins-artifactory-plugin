package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.jfrog.hudson.util.plugins.PluginsUtils;

import java.util.ArrayList;
import java.util.List;

public class GetArtifactoryServerExecutor implements Executor {

    private org.jfrog.hudson.pipeline.common.types.ArtifactoryServer artifactoryServer;
    private final String artifactoryServerID;
    private final Run<?, ?> build;

    public GetArtifactoryServerExecutor(Run<?, ?> build, String artifactoryServerID) {
        this.artifactoryServerID = artifactoryServerID;
        this.build = build;
    }

    public org.jfrog.hudson.pipeline.common.types.ArtifactoryServer getArtifactoryServer() {
        return artifactoryServer;
    }

    @Override
    public void execute() {
        if (StringUtils.isEmpty(artifactoryServerID)) {
            throw new ServerNotFoundException("Artifactory server name is mandatory");
        }

        List<ArtifactoryServer> artifactoryServers = new ArrayList<>();
        List<ArtifactoryServer> artifactoryConfiguredServers = RepositoriesUtils.getArtifactoryServers();
        if (artifactoryConfiguredServers == null) {
            throw new ServerNotFoundException("No Artifactory servers were configured");
        }
        for (ArtifactoryServer server : artifactoryConfiguredServers) {
            if (server.getServerId().equals(artifactoryServerID)) {
                artifactoryServers.add(server);
            }
        }
        if (artifactoryServers.isEmpty()) {
            throw new ServerNotFoundException("Couldn't find Artifactory server with ID: " + artifactoryServerID);
        }
        if (artifactoryServers.size() > 1) {
            throw new ServerNotFoundException("Duplicate configured Artifactory server ID: " + artifactoryServerID);
        }
        ArtifactoryServer server = artifactoryServers.get(0);
        artifactoryServer = new org.jfrog.hudson.pipeline.common.types.ArtifactoryServer(artifactoryServerID, server.getArtifactoryUrl(), server.getDeploymentThreads());
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

    public static class ServerNotFoundException extends RuntimeException {
        public ServerNotFoundException(String message) {
            super(message);
        }
    }
}
