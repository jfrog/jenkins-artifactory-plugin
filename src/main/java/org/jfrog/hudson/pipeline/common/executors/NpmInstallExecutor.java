package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.npm.NpmInstallCallable;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.NpmBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.NpmResolver;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmInstallExecutor implements Executor {

    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String npmExe;
    private String args;
    private FilePath ws;
    private String path;
    private EnvVars env;
    private Log logger;
    private Run build;

    public NpmInstallExecutor(BuildInfo buildInfo, NpmBuild npmBuild, String npmExe, String args, FilePath ws, String path, EnvVars env, TaskListener listener, Run build) {
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.npmBuild = npmBuild;
        this.npmExe = npmExe;
        this.args = args;
        this.ws = ws;
        this.path = path;
        this.env = env;
        this.logger = new JenkinsBuildInfoLog(listener);
        this.build = build;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    @Override
    public void execute() throws Exception {
        NpmResolver resolver = (NpmResolver) npmBuild.getResolver();
        if (resolver.isEmpty()) {
            throw new IllegalStateException("Resolver must be configured with resolution repository and Artifactory server");
        }
        Build build = ws.act(new NpmInstallCallable(createArtifactoryClientBuilder(resolver), resolver.getRepo(), npmExe, args, path, env, logger));
        if (build == null) {
            throw new RuntimeException("npm build failed");
        }
        buildInfo.append(build);
        buildInfo.setAgentName(Utils.getAgentName(ws));
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
