package org.jfrog.hudson.pipeline.types.deployers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.util.publisher.PublisherContext;

/**
 * Created by Tamirh on 16/08/2016.
 */
public class GradleDeployer extends Deployer {
    private boolean usesPlugin = false;
    private boolean deployMavenDescriptors;
    private boolean deployIvyDescriptors;
    private String ivyPattern = "[organisation]/[module]/ivy-[revision].xml";
    private String artifactPattern = "[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]";
    private boolean mavenCompatible = true;
    private String repo;
    public final static GradleDeployer EMPTY_DEPLOYER;

    static {
        EMPTY_DEPLOYER = createEmptyDeployer();
    }

    public GradleDeployer() {
        super();
    }

    @Override
    public ServerDetails getDetails() {
        RepositoryConf snapshotRepositoryConf = null;
        RepositoryConf releaesRepositoryConf = new RepositoryConf(repo, repo, false);
        return new ServerDetails(this.server.getServerName(), this.server.getUrl(), releaesRepositoryConf, snapshotRepositoryConf, releaesRepositoryConf, snapshotRepositoryConf, "", "");
    }

    @Whitelisted
    public boolean isUsesPlugin() {
        return usesPlugin;
    }

    @Whitelisted
    public void setUsesPlugin(boolean usesPlugin) {
        this.usesPlugin = usesPlugin;
    }

    @Whitelisted
    public boolean isDeployMavenDescriptors() {
        return deployMavenDescriptors;
    }

    @Whitelisted
    public void setDeployMavenDescriptors(boolean deployMavenDescriptors) {
        this.deployMavenDescriptors = deployMavenDescriptors;
    }

    @Whitelisted
    public boolean isDeployIvyDescriptors() {
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
                .matrixParams(buildPropertiesString())
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion());
    }

    private static GradleDeployer createEmptyDeployer() {
        GradleDeployer dummy = new GradleDeployer();
        ArtifactoryServer server = new ArtifactoryServer("http://empty_url", "user", "passwrod");
        dummy.setServer(server);
        dummy.setRepo("empty_repo");
        dummy.setDeployArtifacts(false);
        return dummy;
    }
}
