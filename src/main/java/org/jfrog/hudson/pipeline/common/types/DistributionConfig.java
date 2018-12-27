package org.jfrog.hudson.pipeline.common.types;

import java.io.Serializable;
import java.util.List;

/**
 * Created by yahavi on 12/04/2017.
 */
public class DistributionConfig implements Serializable {
    private String buildName;
    private String buildNumber;
    private boolean publish = true;
    private boolean overrideExistingFiles = false;
    private String gpgPassphrase;
    private boolean async = false;
    private String targetRepo;
    private List<String> sourceRepos;
    private boolean dryRun = false;

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    public boolean isOverrideExistingFiles() {
        return overrideExistingFiles;
    }

    public void setOverrideExistingFiles(boolean overrideExistingFiles) {
        this.overrideExistingFiles = overrideExistingFiles;
    }

    public String getGpgPassphrase() {
        return gpgPassphrase;
    }

    public void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public void setTargetRepo(String targetRepo) {
        this.targetRepo = targetRepo;
    }

    public List<String> getSourceRepos() {
        return sourceRepos;
    }

    public void setSourceRepos(List<String> sourceRepos) {
        this.sourceRepos = sourceRepos;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
