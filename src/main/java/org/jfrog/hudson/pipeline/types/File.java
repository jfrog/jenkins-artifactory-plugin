package org.jfrog.hudson.pipeline.types;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.build.api.BaseBuildFileBean;

import java.io.Serializable;
import java.util.Objects;

public class File implements Serializable {
    private static final long serialVersionUID = 1L;

    private String localPath;
    private String remotePath;
    private String md5;
    private String sha1;

    public File() {
    }

    public File(BaseBuildFileBean baseBuildFileBean) {
        this.localPath = baseBuildFileBean.getLocalPath();
        this.remotePath = baseBuildFileBean.getRemotePath();
        this.md5 = baseBuildFileBean.getMd5();
        this.sha1 = baseBuildFileBean.getSha1();
    }

    @Whitelisted
    public String getLocalPath() {
        return localPath;
    }

    @Whitelisted
    public String getRemotePath() {
        return remotePath;
    }

    @Whitelisted
    public String getMd5() {
        return md5;
    }

    @Whitelisted
    public String getSha1() {
        return sha1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(localPath, remotePath);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || other.getClass() != getClass()) {
            return false;
        }
        if (this == other) {
            return true;
        }
        File otherFile = (File) other;
        return StringUtils.equals(localPath, otherFile.getLocalPath()) && StringUtils.equals(remotePath, otherFile.getRemotePath());
    }

    @Override
    public String toString() {
        return "{localPath='" + localPath + "\', " +
                "remotePath='" + remotePath + "\', " +
                "md5=" + md5 + ", " +
                "sha1=" + sha1 + "}";
    }
}
