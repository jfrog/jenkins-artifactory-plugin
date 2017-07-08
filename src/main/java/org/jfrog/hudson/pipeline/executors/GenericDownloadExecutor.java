package org.jfrog.hudson.pipeline.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.generic.FilesResolverCallable;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.util.List;

/**
 * Created by romang on 4/19/16.
 */
public class GenericDownloadExecutor {
    private final Run build;
    private transient FilePath ws;
    private BuildInfo buildInfo;
    private ArtifactoryServer server;
    private TaskListener listener;

    public GenericDownloadExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, BuildInfo buildInfo) {
        this.build = build;
        this.server = server;
        this.listener = listener;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.ws = ws;
    }

    public BuildInfo execution(String spec) throws IOException, InterruptedException {
        CredentialsConfig preferredResolver = server.getDeployerCredentialsConfig();
        ProxyConfiguration proxyConfiguration = server.getProxyConfiguration(Jenkins.getInstance().proxy);
        List<Dependency> resolvedDependencies =
                ws.act(new FilesResolverCallable(new JenkinsBuildInfoLog(listener),
                        preferredResolver.provideUsername(build.getParent()),
                        preferredResolver.providePassword(build.getParent()),
                        server.getUrl(), spec, proxyConfiguration));
        new BuildInfoAccessor(this.buildInfo).appendPublishedDependencies(resolvedDependencies);
        return this.buildInfo;
    }
    
}
