package org.jfrog.hudson.pipeline.types.buildInfo;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.BuildInfoDeployer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Tamirh on 16/05/2016.
 */
public class BuildInfoAccessor {
    BuildInfo buildInfo;

    public BuildInfoAccessor(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    public void appendPublishedDependencies(List<Dependency> resolvedDependencies) {
        this.buildInfo.appendPublishedDependencies(resolvedDependencies);
    }

    public Map<String, String> getEnvVars() {
        return this.buildInfo.getEnvVars();
    }

    public Map<String, String> getSysVars() {
        return this.buildInfo.getSysVars();
    }

    public List<BuildDependency> getBuildDependencies() {
        return this.buildInfo.getBuildDependencies();
    }

    public Date getStartDate() {
        return this.buildInfo.getStartDate();
    }

    public String getBuildName() {
        return this.buildInfo.getName();
    }

    public String getBuildNumber() {
        return this.buildInfo.getNumber();
    }

    public BuildRetention getRetention() {
        return this.buildInfo.getRetention();
    }

    public void captureVariables(EnvVars envVars, Run build, TaskListener listener) throws Exception {
        Env env = this.buildInfo.getEnv();
        if (env.isCapture()) {
            env.collectVariables(envVars, build, listener);
        }
    }

    public void appendDeployedArtifacts(List<Artifact> artifacts) {
        this.buildInfo.appendDeployedArtifacts(artifacts);
    }

    public BuildInfoDeployer createDeployer(Run build, TaskListener listener, Launcher launcher, ArtifactoryServer server)
            throws InterruptedException, NoSuchAlgorithmException, IOException {
        return this.buildInfo.createDeployer(build, listener, launcher, server);
    }

    public List<Module> getModules() {
        return this.buildInfo.getModules();
    }
}
