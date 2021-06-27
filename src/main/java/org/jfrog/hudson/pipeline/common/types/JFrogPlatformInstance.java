package org.jfrog.hudson.pipeline.common.types;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serializable;

/**
 * Represents an instance of JFrog Platform from pipeline script.
 */
public class JFrogPlatformInstance implements Serializable {
    private final ArtifactoryServer artifactoryServer;
    private final DistributionServer distributionServer;
    private String id;
    private String url;

    public JFrogPlatformInstance() {
        artifactoryServer = new ArtifactoryServer();
        distributionServer = new DistributionServer();
    }

    public JFrogPlatformInstance(ArtifactoryServer artifactoryServer, DistributionServer distributionServer, String url, String id) {
        this.id = id;
        this.url = StringUtils.removeEnd(url, "/");
        this.artifactoryServer = artifactoryServer;
        this.distributionServer = distributionServer;
    }

    @Whitelisted
    public ArtifactoryServer getArtifactoryServer() {
        return artifactoryServer;
    }

    @Whitelisted
    public DistributionServer getDistributionServer() {
        return distributionServer;
    }

    public void setCpsScript(CpsScript cpsScript) {
        if (artifactoryServer != null) {
            artifactoryServer.setCpsScript(cpsScript);
        }
        if (distributionServer != null) {
            distributionServer.setCpsScript(cpsScript);
        }
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
