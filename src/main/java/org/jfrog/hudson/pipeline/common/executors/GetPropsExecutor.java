package org.jfrog.hudson.pipeline.common.executors;

import com.google.common.collect.ArrayListMultimap;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.generic.GetPropertiesCallable;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.util.List;

public class GetPropsExecutor implements Executor {
    private final Run build;
    private transient FilePath ws;
    private ArtifactoryServer server;
    private TaskListener listener;
    private String relativePath;
    private List<String> propertyKeys;
    private ArrayListMultimap<String, String> properties;

    public GetPropsExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, String relativePath, List<String> propertyKeys) {
        this.build = build;
        this.server = server;
        this.listener = listener;
        this.relativePath = relativePath;
        this.propertyKeys = propertyKeys;
        this.ws = ws;
    }

    public void execute() throws IOException, InterruptedException {
        CredentialsConfig preferredDeployer = server.getDeployerCredentialsConfig();
        properties = ws.act(new GetPropertiesCallable(new JenkinsBuildInfoLog(listener),
                preferredDeployer.provideCredentials(build.getParent()),
                server.getArtifactoryUrl(), relativePath, Utils.getProxyConfiguration(server), propertyKeys));
    }

    public ArrayListMultimap<String, String> getProperties() {
        return properties;
    }
}
