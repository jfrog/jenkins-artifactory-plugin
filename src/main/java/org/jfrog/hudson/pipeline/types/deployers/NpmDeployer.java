package org.jfrog.hudson.pipeline.types.deployers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

public class NpmDeployer extends Deployer {
    private String repo;

    @Override
    public ServerDetails getDetails() {
        RepositoryConf repositoryConf = new RepositoryConf(repo, repo, false);
        if (server != null) {
            return new ServerDetails(server.getServerName(), server.getUrl(), repositoryConf, null, repositoryConf, null, "", "");
        }
        return new ServerDetails("", "", repositoryConf, null, repositoryConf, null, "", "");
    }

    @Whitelisted
    public void deployArtifacts(BuildInfo buildInfo) {
        throw new IllegalStateException("'deployArtifacts' is not supported in npm. Please use 'publish' instead.");
    }

    @Whitelisted
    public String getRepo() {
        return repo;
    }

    @Whitelisted
    public void setRepo(Object repo) {
        this.repo = Utils.parseJenkinsArg(repo);
    }

    @Override
    public boolean isEmpty() {
        return server == null || StringUtils.isEmpty(repo);
    }

    @Override
    public String getTargetRepository(String deployPath) {
        return repo;
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
