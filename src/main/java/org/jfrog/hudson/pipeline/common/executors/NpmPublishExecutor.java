package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmDeployer;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.NpmBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.NpmResolver;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmPublishExecutor extends NpmExecutor {

    public NpmPublishExecutor(TaskListener listener, BuildInfo buildInfo, Launcher launcher, NpmBuild npmBuild, String javaArgs, String npmExe, String path, FilePath ws, EnvVars env, Run build) {
        super(buildInfo, launcher, npmBuild, javaArgs, npmExe, ws, path, env, listener, build);
    }

    @Override
    public void execute() throws Exception {
        NpmDeployer deployer = (NpmDeployer) npmBuild.getDeployer();
        if (deployer.isEmpty()) {
            throw new IllegalStateException("Deployer must be configured with deployment repository and Artifactory server");
        }
        super.execute(deployer, new NpmResolver(), "", "NpmPublish");
    }
}
