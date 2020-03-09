package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.generic.GenericArtifactsDeployer;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

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
    private String moduleName;

    public GenericUploadExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, BuildInfo buildInfo, StepContext context, String spec, boolean failNoOp, String moduleName) {
        this.server = server;
        this.listener = listener;
        this.build = build;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.ws = ws;
        this.context = context;
        this.spec = spec;
        this.failNoOp = failNoOp;
        this.moduleName = moduleName;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void execute() throws IOException, InterruptedException {
        Credentials credentials = server.getDeployerCredentialsConfig().provideCredentials(build.getParent());
        ProxyConfiguration proxyConfiguration = Utils.getProxyConfiguration(server);

        new BuildInfoAccessor(buildInfo).appendVcs(Utils.extractVcs(ws, new JenkinsBuildInfoLog(listener)));

        List<Artifact> deployedArtifacts = ws.act(new GenericArtifactsDeployer.FilesDeployerCallable(listener, spec,
                server, credentials, Utils.getPropertiesMap(buildInfo, build, context), proxyConfiguration, server.getDeploymentThreads()));
        if (failNoOp && deployedArtifacts.isEmpty()) {
            throw new RuntimeException("Fail-no-op: No files were affected in the upload process.");
        }
        String moduleId = StringUtils.isNotBlank(moduleName) ? moduleName : buildInfo.getName();
        new BuildInfoAccessor(buildInfo).appendArtifacts(deployedArtifacts, moduleId);
    }
}
