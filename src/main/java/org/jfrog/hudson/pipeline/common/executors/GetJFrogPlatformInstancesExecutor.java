package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.util.RepositoriesUtils;

import java.util.ArrayList;
import java.util.List;

public class GetJFrogPlatformInstancesExecutor implements Executor {

    private final String jfrogServersID;
    private final Run<?, ?> build;
    private org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance JFrogPlatformInstance;

    public GetJFrogPlatformInstancesExecutor(Run<?, ?> build, String jfrogServersID) {
        this.jfrogServersID = jfrogServersID;
        this.build = build;
    }

    public org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance getJFrogPlatformInstance() {
        return JFrogPlatformInstance;
    }

    @Override
    public void execute() {
        if (StringUtils.isEmpty(jfrogServersID)) {
            throw new ServerNotFoundException("JFrog Instance ID is mandatory");
        }
        List<JFrogPlatformInstance> jfrogInstancesFound = new ArrayList<>();
        List<JFrogPlatformInstance> jfrogInstances = RepositoriesUtils.getJFrogPlatformInstances();
        if (jfrogInstances == null) {
            throw new ServerNotFoundException("No JFrog Instances were configured");
        }
        for (JFrogPlatformInstance instance : jfrogInstances) {
            if (instance.getId().equals(jfrogServersID)) {
                jfrogInstancesFound.add(instance);
            }
        }
        if (jfrogInstancesFound.isEmpty()) {
            throw new ServerNotFoundException("Couldn't find JFrog Instance ID: " + jfrogServersID);
        }
        if (jfrogInstancesFound.size() > 1) {
            throw new ServerNotFoundException("Duplicate configured JFrog instance ID: " + jfrogServersID);
        }
        JFrogPlatformInstance JFrogPlatformInstance = jfrogInstancesFound.get(0);
        ArtifactoryServer artifactoryServer = new ArtifactoryServer(JFrogPlatformInstance.getArtifactoryServer(), build.getParent());
        this.JFrogPlatformInstance = new org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance(artifactoryServer, JFrogPlatformInstance.getUrl(), JFrogPlatformInstance.getId());
        artifactoryServer.setJfrogServers(this.JFrogPlatformInstance);
    }

    public static class ServerNotFoundException extends RuntimeException {
        public ServerNotFoundException(String message) {
            super(message);
        }
    }
}
