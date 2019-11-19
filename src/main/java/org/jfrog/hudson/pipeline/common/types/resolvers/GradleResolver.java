package org.jfrog.hudson.pipeline.common.types.resolvers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.ResolverContext;

/**
 * Created by Tamirh on 16/08/2016.
 */
public class GradleResolver extends Resolver {
    private String repo;

    @Whitelisted
    public String getRepo() {
        return repo;
    }

    @Whitelisted
    public void setRepo(String repo) {
        this.repo = repo;
    }

    public ServerDetails getResolverDetails() {
        RepositoryConf snapshotRepositoryConf = null;
        RepositoryConf releaesRepositoryConf = new RepositoryConf(repo, repo, false);
        return new ServerDetails(this.server.getServerName(), this.server.getUrl(), releaesRepositoryConf, snapshotRepositoryConf, releaesRepositoryConf, snapshotRepositoryConf, "", "");
    }

    @JsonIgnore
    public ResolverContext getResolverContext(Run build) {
        ResolverContext resolverContext = null;
        if (StringUtils.isNotBlank(repo)) {
            CredentialsConfig resolverCredentials = CredentialManager.getPreferredResolver(
                    this, getArtifactoryServer());
            resolverContext = new ResolverContext(getArtifactoryServer(), getResolverDetails(),
                    resolverCredentials.provideCredentials(build.getParent()), this);
        }
        return resolverContext;
    }

    public boolean isEmpty() {
        return server == null || StringUtils.isEmpty(repo);
    }
}

