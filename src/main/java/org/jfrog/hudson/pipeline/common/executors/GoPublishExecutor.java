package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.CommonDeployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;

public class GoPublishExecutor extends GoExecutor {
    private final String version;

    public GoPublishExecutor(TaskListener listener, BuildInfo buildInfo, Launcher launcher, GoBuild goBuild, String javaArgs, String path, String module, FilePath ws, EnvVars env, Run build, String version) {
        super(buildInfo, launcher, goBuild, javaArgs, ws, path, module, env, listener, build);
        this.version = StringUtils.defaultIfEmpty(version, "");
    }

    @Override
    public void execute() throws Exception {
        CommonDeployer deployer = (CommonDeployer) goBuild.getDeployer();
        if (deployer.isEmpty()) {
            throw new IllegalStateException("Deployer must be configured with deployment repository and Artifactory server");
        }
        super.execute(deployer, new CommonResolver(), "", version, "GoPublish");
    }
}
