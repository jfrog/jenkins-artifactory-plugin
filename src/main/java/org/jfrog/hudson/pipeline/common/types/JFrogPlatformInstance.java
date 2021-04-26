package org.jfrog.hudson.pipeline.common.types;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serializable;

/**
 * Represents an instance of jfrog server instance from pipeline script.
 */
public class JFrogPlatformInstance implements Serializable {
    private final ArtifactoryServer artifactoryServer;
    private String id;
    private String url;
    private CpsScript cpsScript;

    public JFrogPlatformInstance(ArtifactoryServer artifactoryServer, String url, String id) {
        this.id = id;
        this.url = StringUtils.removeEnd(url, "/");
        this.artifactoryServer = artifactoryServer;
    }

    public ArtifactoryServer getArtifactoryServer() {
        return artifactoryServer;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
