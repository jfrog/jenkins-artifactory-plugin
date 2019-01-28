package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.npm.NpmPublishCallable;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmDeployer;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.NpmBuild;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmPublishExecutor implements Executor {

    private StepContext context;
    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String npmExe;
    private FilePath ws;
    private String path;
    private EnvVars env;
    private Log logger;
    private Run build;

    public NpmPublishExecutor(StepContext context, BuildInfo buildInfo, NpmBuild npmBuild, String npmExe, String path, FilePath ws, EnvVars env, TaskListener listener, Run build) {
        this.context = context;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.npmBuild = npmBuild;
        this.npmExe = npmExe;
        this.path = path;
        this.ws = ws;
        this.env = env;
        this.logger = new JenkinsBuildInfoLog(listener);
        this.build = build;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    @Override
    public void execute() throws Exception {
        NpmDeployer deployer = (NpmDeployer) npmBuild.getDeployer();
        if (deployer.isEmpty()) {
            throw new IllegalStateException("Deployer must be configured with deployment repository and Artifactory server");
        }
        Build build = ws.act(new NpmPublishCallable(createArtifactoryClientBuilder(deployer), Utils.getPropertiesMap(buildInfo, this.build, context), deployer.getRepo(), npmExe, path, env, logger));
        if (build == null) {
            throw new RuntimeException("npm publish failed");
        }
        buildInfo.append(build);
        buildInfo.setAgentName(Utils.getAgentName(ws));
    }

    private ArtifactoryBuildInfoClientBuilder createArtifactoryClientBuilder(Deployer deployer) {
        ArtifactoryServer server = deployer.getArtifactoryServer();
        CredentialsConfig preferredDeployer = server.getDeployerCredentialsConfig();
        return new ArtifactoryBuildInfoClientBuilder()
                .setArtifactoryUrl(server.getUrl())
                .setUsername(preferredDeployer.provideUsername(build.getParent()))
                .setPassword(preferredDeployer.providePassword(build.getParent()))
                .setProxyConfiguration(ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy))
                .setLog(logger)
                .setConnectionRetry(server.getConnectionRetry())
                .setConnectionTimeout(server.getTimeout());
    }
}
