package org.jfrog.hudson.pipeline.common.types.resolvers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;

public class NpmResolver extends Resolver {
    private String repo;

    @Whitelisted
    public String getRepo() {
        return repo;
    }

    @Whitelisted
    public void setRepo(String repo) {
        this.repo = repo;
    }

    @Override
    public ServerDetails getResolverDetails() {
        RepositoryConf snapshotRepositoryConf = null;
        RepositoryConf releaesRepositoryConf = new RepositoryConf(repo, repo, false);
        return new ServerDetails(this.server.getServerName(), this.server.getUrl(), releaesRepositoryConf, snapshotRepositoryConf, releaesRepositoryConf, snapshotRepositoryConf, "", "");
    }

    @Override
    public boolean isEmpty() {
        return server == null || StringUtils.isEmpty(repo);
    }
}
