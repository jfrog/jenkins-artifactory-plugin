package org.jfrog.hudson.pipeline.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Module;
import org.jfrog.hudson.npm.NpmInstallCallable;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.types.resolvers.NpmResolver;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmInstallExecutor {

    private TaskListener listener;
    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String args;
    private FilePath ws;
    private Run build;

    public NpmInstallExecutor(TaskListener listener, BuildInfo buildInfo, NpmBuild npmBuild, String args, String path, FilePath ws, Run build) {
        this.listener = listener;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.npmBuild = npmBuild;
        this.args = args;
        this.ws = StringUtils.isBlank(path) ? ws : ws.child(path.replaceFirst("^~", System.getProperty("user.home")));
        this.build = build;
    }

    public BuildInfo execute() throws Exception {
        NpmResolver resolver = npmBuild.getResolver();
        if (resolver.isEmpty()) {
            throw new IllegalStateException("Resolver must be configured with resolution repository and Artifactory server");
        }
        Module npmModule = ws.act(new NpmInstallCallable(build, listener, npmBuild.getExecutablePath(), resolver, args));
        if (npmModule == null) {
            throw new RuntimeException("npm build failed");
        }
        new BuildInfoAccessor(buildInfo).addModule(npmModule);
        buildInfo.setAgentName(Utils.getAgentName(ws));
        return buildInfo;
    }
}
