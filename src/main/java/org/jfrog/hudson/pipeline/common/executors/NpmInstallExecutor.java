package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmGoDeployer;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.NpmGoResolver;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmInstallExecutor extends NpmExecutor {

    private String args;

    public NpmInstallExecutor(BuildInfo buildInfo, Launcher launcher, NpmBuild npmBuild, String javaArgs, String npmExe, String args, FilePath ws, String path, EnvVars env, TaskListener listener, Run build) {
        super(buildInfo, launcher, npmBuild, javaArgs, npmExe, ws, path, env, listener, build);
        this.args = StringUtils.defaultIfEmpty(args, "");
    }

    @Override
    public void execute() throws Exception {
        NpmGoResolver resolver = (NpmGoResolver) npmBuild.getResolver();
        if (resolver.isEmpty()) {
            throw new IllegalStateException("Resolver must be configured with resolution repository and Artifactory server");
        }
        super.execute(new NpmGoDeployer(), resolver, args, "NpmInstall");
    }
}
