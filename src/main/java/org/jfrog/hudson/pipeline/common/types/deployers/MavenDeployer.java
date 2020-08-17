package org.jfrog.hudson.pipeline.common.types.deployers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hudson.model.Run;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.action.DeployedMavenArtifactsAction;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public MavenDeployer setReleaseRepo(String releaseRepo) {
        this.releaseRepo = releaseRepo;
        return this;
    }

    @Whitelisted
    public MavenDeployer setSnapshotRepo(String snapshotRepo) {
        this.snapshotRepo = snapshotRepo;
        return this;
    }

    @Whitelisted
    public String getSnapshotRepo() {
        return snapshotRepo;
    }

    @Whitelisted
    public Deployer setDeployEvenIfUnstable(boolean deployEvenIfUnstable) {
        this.deployEvenIfUnstable = deployEvenIfUnstable;
        return this;
    }

    /**
     * @return True if should deploy artifacts even when the build is unstable (test failures).
     */
    @Whitelisted
    public boolean isDeployEvenIfUnstable() {
        return deployEvenIfUnstable;
    }

    @Override
    @JsonIgnore
    public ServerDetails getDetails() {
        RepositoryConf snapshotRepositoryConf = new RepositoryConf(snapshotRepo, snapshotRepo, false);
        RepositoryConf releaseRepositoryConf = new RepositoryConf(releaseRepo, releaseRepo, false);
        return new ServerDetails(this.server.getServerName(), this.server.getUrl(), releaseRepositoryConf, snapshotRepositoryConf, releaseRepositoryConf, snapshotRepositoryConf, "", "");
    }

    @Override
    @JsonIgnore
    public PublisherContext.Builder getContextBuilder() throws UnsupportedEncodingException {
        return new PublisherContext.Builder().artifactoryServer(getArtifactoryServer())
                .deployerOverrider(this)
                .serverDetails(getDetails())
                .deployArtifacts(isDeployArtifacts())
                .threads(getThreads())
                .evenIfUnstable(isDeployEvenIfUnstable())
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion())
                .includeEnvVars(isIncludeEnvVars())
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .deploymentProperties(DeploymentUrlUtils.buildMatrixParamsString(getProperties(), false))
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
        dummy.setThreads(1);
        return dummy;
    }

    /**
     * Adds artifacts from the provided modules to the Deployed Maven Artifacts Summary Action.
     */
    public static void addDeployedArtifactsActionFromModules(Run build, String artifactoryUrl, List<Module> modules) {
        for (Module module : modules) {
            if (module.getArtifacts() != null) {
                addDeployedArtifactsAction(build, artifactoryUrl, module.getArtifacts());
            }
        }
    }

    /**
     * Adds artifacts from the provided DeployDetails map to the Deployed Maven Artifacts Summary Action.
     */
    public static void addDeployedArtifactsActionFromDetails(Run build, String artifactoryUrl, Map<String, Set<DeployDetails>> deployableArtifactsByModule) {
        deployableArtifactsByModule.forEach((module, detailsSet) -> {
            boolean isMaven = true;
            List<Artifact> curArtifacts = Lists.newArrayList();
            for (DeployDetails curDetails : detailsSet) {
                isMaven = (curDetails.getPackageType() == DeployDetails.PackageType.MAVEN) && isMaven;
                Artifact artifact = new ArtifactBuilder(FilenameUtils.getName(curDetails.getArtifactPath()))
                        .md5(curDetails.getMd5())
                        .sha1(curDetails.getSha1())
                        .type(FilenameUtils.getExtension(curDetails.getArtifactPath()))
                        .remotePath(curDetails.getTargetRepository() + "/" + curDetails.getArtifactPath())
                        .build();
                curArtifacts.add(artifact);
            }
            if (isMaven) {
                addDeployedArtifactsAction(build, artifactoryUrl, curArtifacts);
            }
        });
    }

    /**
     * Adds the provided artifacts to the Deployed Maven Artifacts Summary Action.
     * If such action was not initialized yet, initialize a new one.
     */
    public static void addDeployedArtifactsAction(Run build, String artifactoryUrl, List<Artifact> mavenArtifacts) {
        synchronized (build.getActions()) {
            DeployedMavenArtifactsAction action = build.getAction(DeployedMavenArtifactsAction.class);
            // Initialize action if haven't done so yet.
            if (action == null) {
                action = new DeployedMavenArtifactsAction(build);
                build.getActions().add(action);
            }
            action.addDeployedMavenArtifacts(artifactoryUrl, mavenArtifacts);
        }
    }
}
