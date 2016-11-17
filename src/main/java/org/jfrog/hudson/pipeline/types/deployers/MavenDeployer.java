package org.jfrog.hudson.pipeline.types.deployers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.util.publisher.PublisherContext;

/**
 * Created by Tamirh on 16/08/2016.
 */
public class MavenDeployer extends Deployer {
    private String snapshotRepo;
    private String releaseRepo;
    private boolean evenIfUnstable;
    public final static MavenDeployer EMPTY_DEPLOYER;

    static {
        EMPTY_DEPLOYER = createEmptyDeployer();
    }

    @Whitelisted
    public boolean isEvenIfUnstable() {
        return evenIfUnstable;
    }

    @Whitelisted
    public Deployer setEvenIfUnstable(boolean evenIfUnstable) {
        this.evenIfUnstable = evenIfUnstable;
        return this;
    }

    @Whitelisted
    public String getReleaseRepo() {
        return releaseRepo;
    }

    @Whitelisted
    public Deployer setReleaseRepo(String releaseRepo) {
        this.releaseRepo = releaseRepo;
        return this;
    }

    @Whitelisted
    public Deployer setSnapshotRepo(String snapshotRepo) {
        this.snapshotRepo = snapshotRepo;
        return this;
    }

    @Whitelisted
    public String getSnapshotRepo() {
        return snapshotRepo;
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
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion())
                .includeEnvVars(isIncludeEnvVars())
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .matrixParams(buildPropertiesString())
                .includesExcludes(getArtifactsIncludeExcludeForDeyployment());
    }

    public boolean isEmpty() {
        return server == null || (StringUtils.isEmpty(releaseRepo) && StringUtils.isEmpty(snapshotRepo));
    }

    private static MavenDeployer createEmptyDeployer() {
        MavenDeployer dummy = new MavenDeployer();
        ArtifactoryServer server = new ArtifactoryServer("http://empty_url", "user", "passwrod");
        dummy.setServer(server);
        dummy.setReleaseRepo("empty_repo");
        dummy.setSnapshotRepo("empty_repo");
        dummy.setDeployArtifacts(false);
        return dummy;
    }
}
