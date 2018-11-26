package org.jfrog.hudson.pipeline.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.Module;
import org.jfrog.hudson.npm.NpmInstallCallable;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.ExtractorUtils;

import java.util.Objects;

public class NpmInstallExecutor {

    private TaskListener listener;
    private EnvVars extendedEnv;
    private BuildInfo buildInfo;
    private String args;
    private NpmBuild npmBuild;
    private Launcher launcher;
    private String rootDir;
    private FilePath ws;
    private Run build;

    public NpmInstallExecutor(TaskListener listener, EnvVars env, BuildInfo buildInfo, String args, NpmBuild npmBuild, Launcher launcher, String rootDir, FilePath ws, Run build) {
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.rootDir = Objects.toString(rootDir, "");
        this.extendedEnv = new EnvVars(env);
        this.args = args;
        this.listener = listener;
        this.npmBuild = npmBuild;
        this.launcher = launcher;
        this.build = build;
        this.ws = ws;
    }

    public BuildInfo execute() throws Exception {
        npmBuild.getDeployer().createPublisherBuildInfoDetails(buildInfo);
        String revision = Utils.extractVcsRevision(new FilePath(ws, rootDir));
        extendedEnv.put(ExtractorUtils.GIT_COMMIT, revision);
        EnvExtractor envExtractor = new EnvExtractor(build, buildInfo, npmBuild.getDeployer(), npmBuild.getResolver(), listener, launcher);
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(launcher, ws);
        envExtractor.buildEnvVars(tempDir, extendedEnv);

        Module npmModule = ws.act(new NpmInstallCallable(build, npmBuild.getExecutablePath(), npmBuild.getResolver(), args, listener));
        if (npmModule == null) {
            throw new RuntimeException("npm build failed");
        }

        new BuildInfoAccessor(buildInfo).getModules().add(npmModule);

        // Read the deployable artifacts list from the 'json' file in the agent and append them to the buildInfo object.
        buildInfo.appendDeployableArtifacts(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS), tempDir, listener);
        buildInfo.setAgentName(Utils.getAgentName(ws));
        return buildInfo;
    }
}
