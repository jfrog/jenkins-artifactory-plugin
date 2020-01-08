package org.jfrog.hudson;
import java.text.ParseException;
import java.io.Serializable;
import org.jfrog.build.api.Build;

/**
 * Created by yahavi on 28/03/2017.
 */
public class PublishedBuildDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private String artifactoryUrl;
    private String buildName;
    private String buildNumber;
    private String timestamp;
    private boolean isArtifactoryUnify;

    public PublishedBuildDetails(String artifactoryUrl, String buildName, String buildNumber, String timestamp, boolean isArtifactoryUnify) {
        this.artifactoryUrl = artifactoryUrl;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.timestamp = timestamp;
        this.isArtifactoryUnify = isArtifactoryUnify;
    }

    public String getBuildInfoUrl() {
        try {
            return Build.createBuildInfoUrl(artifactoryUrl, buildName, buildNumber, timestamp, isArtifactoryUnify);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDisplayName() {
        return this.buildName + " / " + this.buildNumber;
    }
}