package org.jfrog.hudson.pipeline.types.deployers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.Utils;
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

    public GradleDeployer() {
        super();
    }

    @Override
    public ServerDetails getDetails() {
        RepositoryConf releaesRepositoryConf = new RepositoryConf(repo, repo, false);
        if (server != null) {
            return new ServerDetails(server.getServerName(), server.getUrl(), releaesRepositoryConf, null, releaesRepositoryConf, null, "", "");
        }
        return new ServerDetails("", "", releaesRepositoryConf, null, releaesRepositoryConf, null, "", "");
    }

    @Whitelisted
    public Boolean isDeployMavenDescriptors() {
        return deployMavenDescriptors;
    }

    @Whitelisted
    public void setDeployMavenDescriptors(Object deployMavenDescriptors) {
        this.deployMavenDescriptors = Boolean.getBoolean(Utils.parseJenkinsArg(deployMavenDescriptors));
    }

    @Whitelisted
    public Boolean isDeployIvyDescriptors() {
        return deployIvyDescriptors;
    }

    @Whitelisted
    public void setDeployIvyDescriptors(Object deployIvyDescriptors) {
        this.deployIvyDescriptors = Boolean.getBoolean(Utils.parseJenkinsArg(deployIvyDescriptors));
    }

    @Whitelisted
    public String getIvyPattern() {
        return ivyPattern;
    }

    @Whitelisted
    public void setIvyPattern(Object ivyPattern) {
        this.ivyPattern = Utils.parseJenkinsArg(ivyPattern);
    }

    @Whitelisted
    public String getArtifactPattern() {
        return artifactPattern;
    }

    @Whitelisted
    public void setArtifactPattern(Object artifactPattern) {
        this.artifactPattern = Utils.parseJenkinsArg(artifactPattern);
    }

    @Whitelisted
    public boolean getMavenCompatible() {
        return mavenCompatible;
    }

    @Whitelisted
    public void setMavenCompatible(Object mavenCompatible) {
        this.mavenCompatible = Boolean.getBoolean(Utils.parseJenkinsArg(mavenCompatible));
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
    public void setRepo(Object repo) {
        this.repo = Utils.parseJenkinsArg(repo);
    }

    public boolean isEmpty() {
        return server == null || StringUtils.isEmpty(repo);
    }

    public String getTargetRepository(String deployPath) {
        return repo;
    }

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
