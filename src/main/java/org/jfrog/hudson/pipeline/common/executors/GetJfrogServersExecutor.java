package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.JfrogServers;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.util.RepositoriesUtils;

import java.util.ArrayList;
import java.util.List;

public class GetJfrogServersExecutor implements Executor {

    private final String jfrogServersID;
    private final Run<?, ?> build;
    private org.jfrog.hudson.pipeline.common.types.JfrogServers jfrogServers;

    public GetJfrogServersExecutor(Run<?, ?> build, String jfrogServersID) {
        this.jfrogServersID = jfrogServersID;
        this.build = build;
    }

    public org.jfrog.hudson.pipeline.common.types.JfrogServers getAJfrogServers() {
        return jfrogServers;
    }

    @Override
    public void execute() {
        if (StringUtils.isEmpty(jfrogServersID)) {
            throw new ServerNotFoundException("Jfrog Instance ID is mandatory");
        }
        List<JfrogServers> jfrogInstancesFound = new ArrayList<>();
        List<JfrogServers> jfrogInstances = RepositoriesUtils.getJfrogInstances();
        if (jfrogInstances == null) {
            throw new ServerNotFoundException("No Jfrog Instances were configured");
        }
        for (JfrogServers instance : jfrogInstances) {
            if (instance.getId().equals(jfrogServersID)) {
                jfrogInstancesFound.add(instance);
            }
        }
        if (jfrogInstancesFound.isEmpty()) {
            throw new ServerNotFoundException("Couldn't find Jfrog Instance ID: " + jfrogServersID);
        }
        if (jfrogInstancesFound.size() > 1) {
            throw new ServerNotFoundException("Duplicate configured Jfrog instance ID: " + jfrogServersID);
        }
        JfrogServers jfrogServers = jfrogInstancesFound.get(0);
        ArtifactoryServer artifactoryServer = new ArtifactoryServer(jfrogServers.getArtifactoryServer(), build.getParent());
        this.jfrogServers = new org.jfrog.hudson.pipeline.common.types.JfrogServers(artifactoryServer, jfrogServers.getPlatformUrl(), jfrogServers.getId());
        artifactoryServer.setJfrogServers(this.jfrogServers);
    }

    public static class ServerNotFoundException extends RuntimeException {
        public ServerNotFoundException(String message) {
            super(message);
        }
    }
}
