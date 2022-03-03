package org.jfrog.hudson;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createBuildInfoUrl;

/**
 * Created by yahavi on 28/03/2017.
 */
public class PublishedBuildDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String artifactoryUrl;
    private final String buildNumber;
    private final String buildName;

    private String startedTimeStamp;
    private String platformUrl;
    private String project;

    public PublishedBuildDetails(String artifactoryUrl, String buildName, String buildNumber) {
        this.artifactoryUrl = artifactoryUrl;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
    }

    public PublishedBuildDetails(String artifactoryUrl, String buildName, String buildNumber, String platformUrl, String startedTimeStamp, String project) {
        this(artifactoryUrl, buildName, buildNumber);
        this.platformUrl = platformUrl;
        this.startedTimeStamp = startedTimeStamp;
        this.project = project;
    }

    /**
     * Get the build info URL.
     * We use encode=false since an encoding already happened in BuildInfoResultAction#createBuildInfoIdentifier.
     *
     * @return build info URL
     */
    public String getBuildInfoUrl() {
        boolean isPlatformUrl = StringUtils.isNotBlank(platformUrl);
        String url = isPlatformUrl ? platformUrl : artifactoryUrl;
        return createBuildInfoUrl(url, buildName, buildNumber, startedTimeStamp, project, false, isPlatformUrl);
    }

    public String getDisplayName() {
        return buildName + " / " + buildNumber;
    }
}