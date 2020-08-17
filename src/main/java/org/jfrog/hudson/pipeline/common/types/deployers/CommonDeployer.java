package org.jfrog.hudson.pipeline.common.types.deployers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

import java.io.UnsupportedEncodingException;

public class CommonDeployer extends Deployer {
    private String repo;

    @Override
    public ServerDetails getDetails() {
        RepositoryConf repositoryConf = new RepositoryConf(repo, repo, false);
        String serverName = server == null ? "" : server.getServerName();
        String url = server == null ? "" : server.getUrl();
        return new ServerDetails(serverName, url, repositoryConf, null, repositoryConf, null, "", "");
    }

    @Whitelisted
    public void deployArtifacts(BuildInfo buildInfo) {
        throw new IllegalStateException("The 'deployArtifacts' method is not supported. Please use the 'publish' method under the relevant build instead.");
    }

    @Whitelisted
    public String getRepo() {
        return repo;
    }

    @Whitelisted
    public void setRepo(String repo) {
        this.repo = repo;
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
    @JsonIgnore
    public PublisherContext.Builder getContextBuilder() throws UnsupportedEncodingException {
        return new PublisherContext.Builder()
                .artifactoryServer(getArtifactoryServer())
                .serverDetails(getDetails())
                .includesExcludes(Utils.getArtifactsIncludeExcludeForDeyployment(getArtifactDeploymentPatterns().getPatternFilter()))
                .skipBuildInfoDeploy(!isDeployBuildInfo())
                .deployerOverrider(this)
                .includeEnvVars(isIncludeEnvVars())
                .deploymentProperties(DeploymentUrlUtils.buildMatrixParamsString(getProperties(), false))
                .artifactoryPluginVersion(ActionableHelper.getArtifactoryPluginVersion());
    }
}
