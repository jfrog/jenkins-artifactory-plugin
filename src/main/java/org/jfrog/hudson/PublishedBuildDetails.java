package org.jfrog.hudson;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.PublishBuildInfo.createBuildInfoUrl;

/**
 * Created by yahavi on 28/03/2017.
 */
public class PublishedBuildDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private final static String ESCAPED_URI_DETECTION = "%20%3A%3A%20";

    private String artifactoryUrl;
    private String buildName;
    private String buildNumber;
    private String platformUrl;
    private String startedTimeStamp;
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
     * Intends to solve https://github.com/jfrog/jenkins-artifactory-plugin/issues/454
     * where URLs became URL encoded twice.
     *
     * @return this but with raw buildName and buildNumbers
     */
    private Object readResolve() {
        if (buildName.contains(ESCAPED_URI_DETECTION)) {
            buildName = safeURLDecoder(buildName);
        }
        if (buildNumber.contains(ESCAPED_URI_DETECTION)) {
            buildNumber = safeURLDecoder(buildNumber);
        }
        return this;
    }

    private String safeURLDecoder(String url) {
        try {
            url = URLDecoder.decode(buildName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return url;
    }

    public String getBuildInfoUrl() throws ParseException {
        if (StringUtils.isNotBlank(platformUrl) && StringUtils.isNotBlank(startedTimeStamp)) {
            return createBuildInfoUrl(platformUrl, buildName, buildNumber, startedTimeStamp, project);
        }
        return createBuildInfoUrl(this.artifactoryUrl, this.buildName, this.buildNumber);
    }

    public String getDisplayName() {
        return this.buildName + " / " + this.buildNumber;
    }
}
