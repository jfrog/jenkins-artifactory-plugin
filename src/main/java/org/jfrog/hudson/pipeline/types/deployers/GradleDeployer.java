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
                .matrixParams(ExtractorUtils.buildPropertiesString(getProperties()))
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion());
    }
}
