package org.jfrog.hudson.pipeline.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.Module;
import org.jfrog.hudson.npm.NpmPublishCallable;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.types.deployers.NpmDeployer;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmPublishExecutor {

    private TaskListener listener;
    private StepContext context;
    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String args;
    private FilePath ws;
    private Run build;

    public NpmPublishExecutor(TaskListener listener, StepContext context, BuildInfo buildInfo, NpmBuild npmBuild, String args, String rootDir, FilePath ws, Run build) {
        this.listener = listener;
        this.context = context;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.npmBuild = npmBuild;
        this.args = args;
        this.ws = StringUtils.isBlank(rootDir) ? ws : ws.child(rootDir);
        this.build = build;
    }

    public BuildInfo execute() throws Exception {
        NpmDeployer deployer = npmBuild.getDeployer();
        if (deployer.isEmpty()) {
            throw new IllegalStateException("Deployer must be configured with deployment repository and Artifactory server");
        }
        Module npmModule = ws.act(new NpmPublishCallable(build, Utils.getPropertiesMap(buildInfo, build, context), npmBuild.getExecutablePath(), deployer, args, listener));
        if (npmModule == null) {
            throw new RuntimeException("npm publish failed");
        }
        new BuildInfoAccessor(buildInfo).getModules().add(npmModule);
        buildInfo.setAgentName(Utils.getAgentName(ws));
        return buildInfo;
    }
}
