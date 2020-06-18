package org.jfrog.hudson.pipeline.common.types.resolvers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;

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
        RepositoryConf releaseRepositoryConf = new RepositoryConf(repo, repo, false);
        return new ServerDetails(this.server.getServerName(), this.server.getUrl(), releaseRepositoryConf, null, releaseRepositoryConf, null, "", "");
    }

    public boolean isEmpty() {
        return server == null || StringUtils.isEmpty(repo);
    }
}

