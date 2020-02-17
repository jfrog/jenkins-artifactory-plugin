package org.jfrog.hudson.pipeline.common.types.buildInfo;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.BuildInfoDeployer;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

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

    public void appendDependencies(List<Dependency> dependencies, String moduleId) {
        Module defaultModule = new ModuleBuilder()
                .id(moduleId)
                .dependencies(dependencies)
                .build();
        Module currentModule = buildInfo.getModules().stream()
                // Check if the default module already exists.
                .filter(module -> StringUtils.equals(module.getId(), moduleId))
                .findAny()
                .orElse(null);
        if (currentModule != null) {
            currentModule.append(defaultModule);
        } else {
            buildInfo.getModules().add(defaultModule);
        }
    }

    public Map<String, String> getEnvVars() {
        return this.buildInfo.getEnvVars();
    }

    public Map<String, String> getSysVars() {
        return this.buildInfo.getSysVars();
    }

    public org.jfrog.build.api.Issues getIssues() {
        return this.buildInfo.getConvertedIssues();
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

    public void appendArtifacts(List<Artifact> artifacts, String moduleId) {
        Module defaultModule = new ModuleBuilder()
                .id(moduleId)
                .artifacts(artifacts)
                .build();
        Module currentModule = buildInfo.getModules().stream()
                // Check if the default module already exists.
                .filter(module -> StringUtils.equals(module.getId(), moduleId))
                .findAny()
                .orElse(null);
        if (currentModule != null) {
            currentModule.append(defaultModule);
        } else {
            buildInfo.getModules().add(defaultModule);
        }
    }

    public ArtifactoryBuildInfoClient createArtifactoryClient(ArtifactoryServer server, Run build, TaskListener listener) {
        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(new ArtifactoryConfigurator(server), server);
        return server.createArtifactoryClient(preferredDeployer.provideCredentials(build.getParent()),
                server.createProxyConfiguration(Jenkins.getInstance().proxy), new JenkinsBuildInfoLog(listener));
    }

    public BuildInfoDeployer createDeployer(Run build, TaskListener listener, ArtifactoryServer server, ArtifactoryBuildInfoClient client)
            throws InterruptedException, NoSuchAlgorithmException, IOException {
        return this.buildInfo.createDeployer(build, listener, new ArtifactoryConfigurator(server), client);
    }

    public List<Module> getModules() {
        return this.buildInfo.getModules();
    }

    public void appendVcs(Vcs vcs) {
        this.buildInfo.appendVcs(vcs);
    }

    public List<Vcs> getVcs() {
        return this.buildInfo.getVcs();
    }
}
