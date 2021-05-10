package org.jfrog.hudson;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.Serializable;
import java.text.ParseException;

/**
 * Created by yahavi on 28/03/2017.
 */
public class PublishedBuildDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artifactoryUrl;
    private String buildName;
    private String buildNumber;
    private String platformUrl;
    private String startedTimeStamp;

    public PublishedBuildDetails(String artifactoryUrl, String buildName, String buildNumber) {
        this.artifactoryUrl = artifactoryUrl;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    public PublishedBuildDetails(String artifactoryUrl, String buildName, String buildNumber, String platformUrl, String startedTimeStamp) {
        this(artifactoryUrl, buildName, buildNumber);
        this.platformUrl = platformUrl;
        this.startedTimeStamp = startedTimeStamp;
    }

    public String getBuildInfoUrl() throws ParseException {
        if (StringUtils.isNotBlank(platformUrl) && StringUtils.isNotBlank(startedTimeStamp)) {
            return ArtifactoryBuildInfoClient.createBuildInfoUrl(platformUrl, buildName, buildNumber, startedTimeStamp);
        }
        return ArtifactoryBuildInfoClient.createBuildInfoUrl(this.artifactoryUrl, this.buildName, this.buildNumber);
    }

    public String getDisplayName() {
        return this.buildName + " / " + this.buildNumber;
    }
}