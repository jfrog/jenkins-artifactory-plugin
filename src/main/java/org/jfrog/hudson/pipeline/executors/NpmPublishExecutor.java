package org.jfrog.hudson.pipeline.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.npm.NpmPublishCallable;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.types.deployers.NpmDeployer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmPublishExecutor {

    private StepContext context;
    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String args;
    private FilePath ws;
    private String path;
    private Log logger;
    private Run build;

    public NpmPublishExecutor(StepContext context, BuildInfo buildInfo, NpmBuild npmBuild, String args, String path, FilePath ws, TaskListener listener, Run build) {
        this.context = context;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.npmBuild = npmBuild;
        this.args = args;
        this.path = path;
        this.ws = ws;
        this.logger = new JenkinsBuildInfoLog(listener);
        this.build = build;
    }

    public BuildInfo execute() throws Exception {
        NpmDeployer deployer = npmBuild.getDeployer();
        if (deployer.isEmpty()) {
            throw new IllegalStateException("Deployer must be configured with deployment repository and Artifactory server");
        }
        Module npmModule = ws.act(new NpmPublishCallable(createArtifactoryClientBuilder(deployer), Utils.getPropertiesMap(buildInfo, build, context), npmBuild.getExecutablePath(), deployer, args, path, logger));
        if (npmModule == null) {
            throw new RuntimeException("npm publish failed");
        }
        new BuildInfoAccessor(buildInfo).addModule(npmModule);
        buildInfo.setAgentName(Utils.getAgentName(ws));
        return buildInfo;
    }

    private ArtifactoryBuildInfoClientBuilder createArtifactoryClientBuilder(NpmDeployer deployer) {
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
