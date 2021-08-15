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

public class GoRunExecutor extends GoExecutor {
    private String args;

    public GoRunExecutor(BuildInfo buildInfo, Launcher launcher, GoBuild goBuild, String javaArgs, String args, FilePath ws, String path, String module, EnvVars env, TaskListener listener, Run build) {
        super(buildInfo, launcher, goBuild, javaArgs, ws, path, module, env, listener, build);
        this.args = StringUtils.defaultIfEmpty(args, "");
    }

    @Override
    public void execute() throws Exception {
        CommonResolver resolver = (CommonResolver) goBuild.getResolver();
        if (resolver.isEmpty()) {
            throw new IllegalStateException("Resolver must be configured with resolution repository and Artifactory server");
        }
        super.execute(new CommonDeployer(), resolver, args, "", "GoRun");
    }
}
