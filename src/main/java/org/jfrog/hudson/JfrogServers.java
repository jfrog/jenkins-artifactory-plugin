package org.jfrog.hudson;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents an instance of jenkins jfrog instance configuration page.
 */
public class JfrogServers {
    private String platformUrl;
    private String id;
    private ArtifactoryServer artifactoryServer;
    private CredentialsConfig deployerCredentialsConfig;
    private CredentialsConfig resolverCredentialsConfig;

    @DataBoundConstructor
    public JfrogServers(String id, String artifactoryUrl, String platformUrl, CredentialsConfig deployerCredentialsConfig,
                        CredentialsConfig resolverCredentialsConfig, int timeout, boolean bypassProxy, Integer connectionRetry, Integer deploymentThreads) {
        this.id = id;
        this.platformUrl = StringUtils.removeEnd(platformUrl, "/");
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.resolverCredentialsConfig = resolverCredentialsConfig;
        artifactoryServer = new ArtifactoryServer(id, artifactoryUrl, deployerCredentialsConfig, resolverCredentialsConfig, timeout, bypassProxy, connectionRetry, deploymentThreads);
    }

    public JfrogServers(ArtifactoryServer artifactoryServer) {
        id = artifactoryServer.getServerId();
        this.artifactoryServer = artifactoryServer;
    }

    public JfrogServers() {
    }

    public JfrogServers(org.jfrog.hudson.pipeline.common.types.ArtifactoryServer artifactoryServer) {
        this("", artifactoryServer.getUrl(), "", artifactoryServer.createCredentialsConfig(), artifactoryServer.createCredentialsConfig(), artifactoryServer.getConnection().getTimeout(), artifactoryServer.isBypassProxy(), artifactoryServer.getConnection().getRetry(), artifactoryServer.getDeploymentThreads());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public ArtifactoryServer getArtifactoryServer() {
        return artifactoryServer;
    }

    public void setArtifactoryServer(ArtifactoryServer artifactoryServer) {
        this.artifactoryServer = artifactoryServer;
    }

    public CredentialsConfig getDeployerCredentialsConfig() {
        return deployerCredentialsConfig;
    }

    public void setDeployerCredentialsConfig(CredentialsConfig deployerCredentialsConfig) {
        this.deployerCredentialsConfig = deployerCredentialsConfig;
    }

    public CredentialsConfig getResolverCredentialsConfig() {
        return resolverCredentialsConfig;
    }

    public void setResolverCredentialsConfig(CredentialsConfig resolverCredentialsConfig) {
        this.resolverCredentialsConfig = resolverCredentialsConfig;
    }
}
