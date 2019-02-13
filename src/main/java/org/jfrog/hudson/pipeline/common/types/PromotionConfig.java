package org.jfrog.hudson.pipeline.common.types;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;

/**
 * Created by romang on 4/21/16.
 */
public class PromotionConfig implements Serializable {

    private String buildName;
    private String buildNumber;
    private String targetRepo;
    private String sourceRepo;
    private String status;
    private String comment;
    private boolean includeDependencies;
    private boolean copy;
    private boolean failFast = true;

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName.trim();
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber.trim();
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(String targetRepo) {
        this.targetRepo = targetRepo.trim();
    }

    public String getSourceRepo() {
        return sourceRepo;
    }

    public void setSourceRepo(String sourceRepo) {
        this.sourceRepo = sourceRepo.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status.trim();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment.trim();
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public boolean isCopy() {
        return copy;
    }

    public void setCopy(boolean copy) {
        this.copy = copy;
    }

    @Whitelisted
    public boolean isFailFast() {
        return failFast;
    }

    @Whitelisted
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}
