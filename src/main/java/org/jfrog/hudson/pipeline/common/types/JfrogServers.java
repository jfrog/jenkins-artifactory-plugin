package org.jfrog.hudson.pipeline.common.types;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serializable;

/**
 * Represents an instance of jfrog server instance from pipeline script.
 */
public class JfrogServers implements Serializable {
    private final ArtifactoryServer artifactoryServer;
    private String id;
    private String platformUrl;
    private CpsScript cpsScript;

    public JfrogServers(ArtifactoryServer artifactoryServer, String platformUrl, String id) {
        this.id = id;
        this.platformUrl = StringUtils.removeEnd(platformUrl, "/");
        this.artifactoryServer = artifactoryServer;
    }

    public ArtifactoryServer getArtifactoryServer() {
        return artifactoryServer;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
