package org.jfrog.hudson.pipeline.types;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.ArtifactoryPipelineConfigurator;
import org.jfrog.hudson.pipeline.PipelineBuildInfoDeployer;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.CredentialManager;

import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by romang on 4/26/16.
 */
public class BuildInfo implements Serializable {

    private String buildName;
    private String buildNumber;
    private Date startDate;

    private Map<Artifact, Artifact> deployedArtifacts = new HashMap<Artifact, Artifact>();
    private List<BuildDependency> buildDependencies = new ArrayList<BuildDependency>();
    private Map<Dependency, Dependency> publishedDependencies = new HashMap<Dependency, Dependency>();
    private PipelineEnv env = new PipelineEnv();

    public BuildInfo(Run build) {
        this.buildName = BuildUniqueIdentifierHelper.getBuildName(build);
        this.buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
    }

    @Whitelisted
    public void setName(String name) {
        this.buildName = name;
    }

    @Whitelisted
    public void setNumber(String number) {
        this.buildNumber = number;
    }

    @Whitelisted
    public String getName() {
        return buildName;
    }

    @Whitelisted
    public String getNumber() {
        return buildNumber;
    }

    @Whitelisted
    public Date getStartDate() {
        return startDate;
    }

    @Whitelisted
    public void setStartDate(Date date) {
        this.startDate = date;
    }

    @Whitelisted
    public void append(BuildInfo other) {
        this.deployedArtifacts.putAll(other.deployedArtifacts);
        this.publishedDependencies.putAll(other.publishedDependencies);
        this.buildDependencies.addAll(other.buildDependencies);
        this.env.append(other.getEnv());
    }

    @Whitelisted
    public PipelineEnv getEnv() {
        return env;
    }

    protected void appendDeployedArtifacts(List<Artifact> artifacts) {
        if (artifacts == null) {
            return;
        }
        for (Artifact artifact : artifacts) {
            deployedArtifacts.put(artifact, artifact);
        }
    }

    protected void appendBuildDependencies(List<BuildDependency> dependencies) {
        if (dependencies == null) {
            return;
        }
        buildDependencies.addAll(dependencies);
    }

    protected void appendPublishedDependencies(List<Dependency> dependencies) {
        if (dependencies == null) {
            return;
        }
        for (Dependency dependency : dependencies) {
            publishedDependencies.put(dependency, dependency);
        }
    }

    protected Map<Artifact, Artifact> getDeployedArtifacts() {
        return deployedArtifacts;
    }

    protected List<BuildDependency> getBuildDependencies() {
        return buildDependencies;
    }

    protected Map<Dependency, Dependency> getPublishedDependencies() {
        return publishedDependencies;
    }

    protected Map<String, String> getEnvVars() {
        return env.getEnvVars();
    }

    protected Map<String, String> getSysVars() {
        return env.getSysVars();
    }

    protected PipelineBuildInfoDeployer createDeployer(Run build, TaskListener listener, ArtifactoryServer server)
            throws InterruptedException, NoSuchAlgorithmException, IOException {
        ArtifactoryPipelineConfigurator config = new ArtifactoryPipelineConfigurator(server);
        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(config, server);
        ArtifactoryBuildInfoClient client = server.createArtifactoryClient(preferredDeployer.getUsername(),
                preferredDeployer.getPassword(), server.createProxyConfiguration(Jenkins.getInstance().proxy));

        return new PipelineBuildInfoDeployer(config, client, build, listener, new PipelineBuildInfoAccessor(this));
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.env.setCpsScript(cpsScript);
    }
}
