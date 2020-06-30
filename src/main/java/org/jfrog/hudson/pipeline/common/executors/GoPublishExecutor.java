package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.go.GoPublishCallable;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmGoDeployer;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;

public class GoPublishExecutor implements Executor {

    private StepContext context;
    private BuildInfo buildInfo;
    private GoBuild goBuild;
    private FilePath ws;
    private String path;
    private String version;
    private String module;
    private Log logger;
    private Run build;

    public GoPublishExecutor(StepContext context, BuildInfo buildInfo, GoBuild goBuild, String path, String version, String module, FilePath ws, TaskListener listener, Run build) {
        this.context = context;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.goBuild = goBuild;
        this.path = path;
        this.version = version;
        this.module = module;
        this.ws = ws;
        this.logger = new JenkinsBuildInfoLog(listener);
        this.build = build;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    @Override
    public void execute() throws Exception {
        NpmGoDeployer deployer = (NpmGoDeployer) goBuild.getDeployer();
        if (deployer.isEmpty()) {
            throw new IllegalStateException("Deployer must be configured with deployment repository and Artifactory server");
        }
        Build build = ws.act(new GoPublishCallable(createArtifactoryClientBuilder(deployer), Utils.getPropertiesMap(buildInfo, this.build, context), deployer.getRepo(), path, version, module, logger));
        if (build == null) {
            throw new RuntimeException("go publish failed");
        }
        buildInfo.append(build);
        buildInfo.setAgentName(Utils.getAgentName(ws));
    }

    private ArtifactoryBuildInfoClientBuilder createArtifactoryClientBuilder(Deployer deployer) {
        ArtifactoryServer server = deployer.getArtifactoryServer();
        Credentials deployerCredentials = server.getDeployerCredentialsConfig().provideCredentials(build.getParent());
        return new ArtifactoryBuildInfoClientBuilder()
                .setArtifactoryUrl(server.getArtifactoryUrl())
                .setUsername(deployerCredentials.getUsername())
                .setPassword(deployerCredentials.getPassword())
                .setAccessToken(deployerCredentials.getAccessToken())
                .setProxyConfiguration(ProxyUtils.createProxyConfiguration())
                .setLog(logger)
                .setConnectionRetry(server.getConnectionRetry())
                .setConnectionTimeout(server.getTimeout());
    }
}
