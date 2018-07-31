package org.jfrog.hudson.pipeline.types.deployers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

/**
 * Created by Tamirh on 16/08/2016.
 */
public class MavenDeployer extends Deployer {
    private String snapshotRepo;
    private String releaseRepo;
    private boolean deployEvenIfUnstable = false;
    public final static MavenDeployer EMPTY_DEPLOYER;

    static {
        EMPTY_DEPLOYER = createEmptyDeployer();
    }

    @Whitelisted
    public String getReleaseRepo() {
        return releaseRepo;
    }

    @Whitelisted
    public Deployer setReleaseRepo(Object releaseRepo) {
        this.releaseRepo = Utils.parseJenkinsArg(releaseRepo);
        return this;
    }

    @Whitelisted
    public Deployer setSnapshotRepo(Object snapshotRepo) {
        this.snapshotRepo = Utils.parseJenkinsArg(snapshotRepo);
        return this;
    }

    @Whitelisted
    public String getSnapshotRepo() {
        return snapshotRepo;
    }

    @Whitelisted
    public Deployer setDeployEvenIfUnstable(Object deployEvenIfUnstable) {
        this.deployEvenIfUnstable = Boolean.getBoolean(Utils.parseJenkinsArg(deployEvenIfUnstable));
        return this;
    }

    /**
     * @return  True if should deploy artifacts even when the build is unstable (test failures).
     */
    @Whitelisted
    public boolean isDeployEvenIfUnstable() {
        return deployEvenIfUnstable;
    }

    @Override
    public ServerDetails getDetails() {
        RepositoryConf snapshotRepositoryConf = new RepositoryConf(snapshotRepo, snapshotRepo, false);
        RepositoryConf releaesRepositoryConf = new RepositoryConf(releaseRepo, releaseRepo, false);
        return new ServerDetails(this.server.getServerName(), this.server.getUrl(), releaesRepositoryConf, snapshotRepositoryConf, releaesRepositoryConf, snapshotRepositoryConf, "", "");
    }

    @Override
    public PublisherContext.Builder getContextBuilder() {
        return new PublisherContext.Builder().artifactoryServer(getArtifactoryServer())
                .deployerOverrider(this)
                .serverDetails(getDetails())
                .deployArtifacts(isDeployArtifacts())
                .evenIfUnstable(isDeployEvenIfUnstable())
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion())
                .includeEnvVars(isIncludeEnvVars())
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .deploymentProperties(ExtractorUtils.buildPropertiesString(getProperties()))
                .includesExcludes(getArtifactsIncludeExcludeForDeyployment());
    }

    public boolean isEmpty() {
        return server == null || (StringUtils.isEmpty(releaseRepo) && StringUtils.isEmpty(snapshotRepo));
    }

    public String getTargetRepository(String deployPath) {
        return StringUtils.isNotBlank(snapshotRepo) && deployPath.contains("-SNAPSHOT") ? snapshotRepo : releaseRepo;
    }

    private static MavenDeployer createEmptyDeployer() {
        MavenDeployer dummy = new MavenDeployer();
        ArtifactoryServer server = new ArtifactoryServer("http://empty_url", "user", "password");
        dummy.setServer(server);
        dummy.setReleaseRepo("empty_repo");
        dummy.setSnapshotRepo("empty_repo");
        dummy.setDeployArtifacts(false);
        dummy.setDeployEvenIfUnstable(false);
        return dummy;
    }
}
