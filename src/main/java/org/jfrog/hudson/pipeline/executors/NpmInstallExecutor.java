package org.jfrog.hudson.pipeline.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.npm.NpmInstallCallable;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.types.resolvers.NpmResolver;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmInstallExecutor {

    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String args;
    private FilePath ws;
    private String path;
    private Log logger;
    private Run build;

    public NpmInstallExecutor(BuildInfo buildInfo, NpmBuild npmBuild, String args, FilePath ws, String path, TaskListener listener, Run build) {
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.npmBuild = npmBuild;
        this.args = args;
        this.ws = ws;
        this.path = path;
        this.logger = new JenkinsBuildInfoLog(listener);
        this.build = build;
    }

    public BuildInfo execute() throws Exception {
        NpmResolver resolver = npmBuild.getResolver();
        if (resolver.isEmpty()) {
            throw new IllegalStateException("Resolver must be configured with resolution repository and Artifactory server");
        }
        Module npmModule = ws.act(new NpmInstallCallable(createArtifactoryClientBuilder(resolver), resolver.getRepo(), npmBuild.getExecutablePath(), args, path, logger));
        if (npmModule == null) {
            throw new RuntimeException("npm build failed");
        }
        new BuildInfoAccessor(buildInfo).addModule(npmModule);
        buildInfo.setAgentName(Utils.getAgentName(ws));
        return buildInfo;
    }

    private ArtifactoryDependenciesClientBuilder createArtifactoryClientBuilder(NpmResolver resolver) {
        ArtifactoryServer server = resolver.getArtifactoryServer();
        CredentialsConfig preferredResolver = server.getResolvingCredentialsConfig();
        return new ArtifactoryDependenciesClientBuilder()
                .setArtifactoryUrl(server.getUrl())
                .setUsername(preferredResolver.provideUsername(build.getParent()))
                .setPassword(preferredResolver.providePassword(build.getParent()))
                .setProxyConfiguration(ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy))
                .setLog(logger)
                .setConnectionRetry(server.getConnectionRetry())
                .setConnectionTimeout(server.getTimeout());
    }
}
