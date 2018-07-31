package org.jfrog.hudson.pipeline.types.resolvers;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.RepositoryConf;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.pipeline.Utils;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class MavenResolver extends Resolver {
    private String snapshotRepo;
    private String releaseRepo;

    @Whitelisted
    public String getSnapshotRepo() {
        return snapshotRepo;
    }

    @Whitelisted
    public void setSnapshotRepo(Object snapshotRepo) {
        this.snapshotRepo = Utils.parseJenkinsArg(snapshotRepo);
    }

    @Whitelisted
    public String getReleaseRepo() {
        return releaseRepo;
    }

    @Whitelisted
    public void setReleaseRepo(Object releaseRepo) {
        this.releaseRepo = Utils.parseJenkinsArg(releaseRepo);
    }

    public ServerDetails getResolverDetails() {
        RepositoryConf snapshotRepositoryConf = new RepositoryConf(snapshotRepo, snapshotRepo, false);
        RepositoryConf releaseRepositoryConf = new RepositoryConf(releaseRepo, releaseRepo, false);
        return new ServerDetails(this.server.getServerName(), this.server.getUrl(), releaseRepositoryConf, snapshotRepositoryConf, releaseRepositoryConf, snapshotRepositoryConf, "", "");
    }

    public boolean isEmpty() {
        return server == null || (StringUtils.isEmpty(releaseRepo) && StringUtils.isEmpty(snapshotRepo));
    }
}
