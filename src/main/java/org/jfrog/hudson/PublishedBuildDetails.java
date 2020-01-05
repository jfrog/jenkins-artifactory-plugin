package org.jfrog.hudson;

import java.io.Serializable;

/**
 * Created by yahavi on 28/03/2017.
 */
public class PublishedBuildDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artifactoryUrl;
    private String buildName;
    private String buildNumber;

    public PublishedBuildDetails(String artifactoryUrl, String buildName, String buildNumber) {
        this.artifactoryUrl = artifactoryUrl;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    public String getBuildInfoUrl() {
        return this.artifactoryUrl + "/webapp/builds/" + this.buildName + "/" + this.buildNumber;
    }

    public String getDisplayName() {
        return this.buildName + " / " + this.buildNumber;
    }
}