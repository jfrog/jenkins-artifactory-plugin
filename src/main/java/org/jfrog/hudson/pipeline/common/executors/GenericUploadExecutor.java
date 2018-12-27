package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.generic.GenericArtifactsDeployer;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.Credentials;

import java.io.IOException;
import java.util.List;

/**
 * Created by romang on 4/24/16.
 */
public class GenericUploadExecutor implements Executor {
    private transient FilePath ws;
    private transient Run build;
    private transient TaskListener listener;
    private BuildInfo buildInfo;
    private boolean failNoOp;
    private ArtifactoryServer server;
    private StepContext context;
    private String spec;

    public GenericUploadExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, BuildInfo buildInfo, StepContext context, String spec, boolean failNoOp) {
        this.server = server;
        this.listener = listener;
        this.build = build;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.ws = ws;
        this.context = context;
        this.spec = spec;
        this.failNoOp = failNoOp;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void execute() throws IOException, InterruptedException {
        Credentials credentials = new Credentials(server.getDeployerCredentialsConfig().provideUsername(build.getParent()),
                server.getDeployerCredentialsConfig().providePassword(build.getParent()));
        ProxyConfiguration proxyConfiguration = Utils.getProxyConfiguration(server);
        List<Artifact> deployedArtifacts = ws.act(new GenericArtifactsDeployer.FilesDeployerCallable(listener, spec,
                server, credentials, Utils.getPropertiesMap(buildInfo, build, context), proxyConfiguration));
        if (failNoOp && deployedArtifacts.isEmpty()) {
            throw new RuntimeException("Fail-no-op: No files were affected in the upload process.");
        }
        new BuildInfoAccessor(buildInfo).appendDeployedArtifacts(deployedArtifacts);
    }
}
