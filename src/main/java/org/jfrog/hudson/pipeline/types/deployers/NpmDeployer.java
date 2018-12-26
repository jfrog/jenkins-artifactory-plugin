package org.jfrog.hudson.pipeline.types.deployers;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

public class NpmDeployer extends Deployer {

    @Whitelisted
    public void deployArtifacts(BuildInfo buildInfo) {
        throw new IllegalStateException("The 'deployArtifacts' method is not supported for npm builds. Please use the 'publish' method instead.");
    }

    @Whitelisted
    public String getRepo() {
        return releaseRepo;
    }

    @Whitelisted
    public void setRepo(String repo) {
        releaseRepo = snapshotRepo = repo;
    }

    @Override
    public PublisherContext.Builder getContextBuilder() {
        return new PublisherContext.Builder()
                .artifactoryServer(getArtifactoryServer())
                .serverDetails(getDetails())
                .includesExcludes(Utils.getArtifactsIncludeExcludeForDeyployment(getArtifactDeploymentPatterns().getPatternFilter()))
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .deployerOverrider(this)
                .includeEnvVars(isIncludeEnvVars())
                .deploymentProperties(ExtractorUtils.buildPropertiesString(getProperties()))
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion());
    }
}
