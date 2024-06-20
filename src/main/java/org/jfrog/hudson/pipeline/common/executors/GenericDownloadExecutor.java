package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.generic.FilesResolverCallable;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.util.List;

/**
 * Created by romang on 4/19/16.
 */
public class GenericDownloadExecutor implements Executor {
    private final Run build;
    private transient FilePath ws;
    private BuildInfo buildInfo;
    private boolean failNoOp;
    private ArtifactoryServer server;
    private TaskListener listener;
    private String spec;
    private String moduleName;
    public final static String FAIL_NO_OP_ERROR_MESSAGE = "Fail-no-op: No files were affected in the download process.";

    public GenericDownloadExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, BuildInfo buildInfo, String spec, boolean failNoOp, String moduleName) {
        this.build = build;
        this.server = server;
        this.listener = listener;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.failNoOp = failNoOp;
        this.ws = ws;
        this.spec = spec;
        this.moduleName = moduleName;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void execute() throws IOException, InterruptedException {
        CredentialsConfig preferredResolver = server.getDeployerCredentialsConfig();
        Credentials resolverCredentials = preferredResolver.provideCredentials(build);
        List<Dependency> resolvedDependencies =
                ws.act(new FilesResolverCallable(new JenkinsBuildInfoLog(listener),
                        resolverCredentials, server.getArtifactoryUrl(), spec, Utils.getProxyConfiguration(server)));
        if (failNoOp && resolvedDependencies.isEmpty()) {
            throw new RuntimeException(FAIL_NO_OP_ERROR_MESSAGE);
        }
        String moduleId = StringUtils.isNotBlank(moduleName) ? moduleName : buildInfo.getName();
        this.buildInfo.appendDependencies(resolvedDependencies, moduleId);
    }
}
