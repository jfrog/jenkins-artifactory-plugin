package org.jfrog.hudson.pipeline.common.types;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serializable;

/**
 * Represents an instance of JFrog Platform from pipeline script.
 */
public class JFrogPlatformInstance implements Serializable {
    private final ArtifactoryServer artifactory;
    private final DistributionServer distribution;
    private String id;
    private String url;

    public JFrogPlatformInstance() {
        artifactory = new ArtifactoryServer();
        distribution = new DistributionServer();
    }

    public JFrogPlatformInstance(ArtifactoryServer artifactory, DistributionServer distribution, String url, String id) {
        this.id = id;
        this.url = StringUtils.removeEnd(url, "/");
        this.artifactory = artifactory;
        this.distribution = distribution;
    }

    @Whitelisted
    public ArtifactoryServer getArtifactory() {
        return artifactory;
    }

    @Whitelisted
    public DistributionServer getDistribution() {
        return distribution;
    }

    public void setCpsScript(CpsScript cpsScript) {
        if (artifactory != null) {
            artifactory.setCpsScript(cpsScript);
        }
        if (distribution != null) {
            distribution.setCpsScript(cpsScript);
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
