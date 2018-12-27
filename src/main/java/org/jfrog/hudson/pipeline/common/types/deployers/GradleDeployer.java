package org.jfrog.hudson.pipeline.common.types.deployers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

/**
 * Created by Tamirh on 16/08/2016.
 */
public class GradleDeployer extends Deployer {
    private Boolean deployMavenDescriptors;
    private Boolean deployIvyDescriptors;
    private String ivyPattern = "[organisation]/[module]/ivy-[revision].xml";
    private String artifactPattern = "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";
    private boolean mavenCompatible = true;
    private String repo;

    @Override
    @JsonIgnore
    public ServerDetails getDetails() {
        RepositoryConf releaesRepositoryConf = new RepositoryConf(repo, repo, false);
        String serverName = server == null ? "" : server.getServerName();
        String url = server == null ? "" : server.getUrl();
        return new ServerDetails(serverName, url, releaesRepositoryConf, null, releaesRepositoryConf, null, "", "");
    }

    @Whitelisted
    public Boolean isDeployMavenDescriptors() {
        return deployMavenDescriptors;
    }

    @Whitelisted
    public void setDeployMavenDescriptors(boolean deployMavenDescriptors) {
        this.deployMavenDescriptors = deployMavenDescriptors;
    }

    @Whitelisted
    public Boolean isDeployIvyDescriptors() {
        return deployIvyDescriptors;
    }

    @Whitelisted
    public void setDeployIvyDescriptors(boolean deployIvyDescriptors) {
        this.deployIvyDescriptors = deployIvyDescriptors;
    }

    @Whitelisted
    public String getIvyPattern() {
        return ivyPattern;
    }

    @Whitelisted
    public void setIvyPattern(String ivyPattern) {
        this.ivyPattern = ivyPattern;
    }

    @Whitelisted
    public String getArtifactPattern() {
        return artifactPattern;
    }

    @Whitelisted
    public void setArtifactPattern(String artifactPattern) {
        this.artifactPattern = artifactPattern;
    }

    @Whitelisted
    public boolean getMavenCompatible() {
        return mavenCompatible;
    }

    @Whitelisted
    public void setMavenCompatible(boolean mavenCompatible) {
        this.mavenCompatible = mavenCompatible;
    }

    @Whitelisted
    public boolean isMavenCompatible() {
        return mavenCompatible;
    }

    @Whitelisted
    public String getRepo() {
        return repo;
    }

    @Whitelisted
    public void setRepo(String repo) {
        this.repo = repo;
    }

    public boolean isEmpty() {
        return server == null || StringUtils.isEmpty(repo);
    }

    public String getTargetRepository(String deployPath) {
        return repo;
    }

    @JsonIgnore
    public PublisherContext.Builder getContextBuilder() {
        return new PublisherContext.Builder()
                .artifactoryServer(getArtifactoryServer())
                .serverDetails(getDetails())
                .deployArtifacts(isDeployArtifacts()).includesExcludes(Utils.getArtifactsIncludeExcludeForDeyployment(getArtifactDeploymentPatterns().getPatternFilter()))
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .artifactsPattern(getArtifactPattern())
                .ivyPattern(getIvyPattern())
                .deployIvy(isDeployIvyDescriptors())
                .deployMaven(isDeployMavenDescriptors())
                .deployerOverrider(this)
                .includeEnvVars(isIncludeEnvVars())
                .maven2Compatible(getMavenCompatible())
                .deploymentProperties(ExtractorUtils.buildPropertiesString(getProperties()))
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion());
    }
}
