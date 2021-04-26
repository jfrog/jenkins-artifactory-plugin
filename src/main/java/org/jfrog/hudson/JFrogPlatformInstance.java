package org.jfrog.hudson;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents an instance of jenkins JFrog instance configuration page.
 */
public class JFrogPlatformInstance {
    public static final int DEFAULT_CONNECTION_TIMEOUT = 300;    // 5 Minutes
    public static final int DEFAULT_DEPLOYMENT_THREADS_NUMBER = 3;

    private String url;
    private String id;
    private ArtifactoryServer artifactoryServer;
    private CredentialsConfig deployerCredentialsConfig;
    private CredentialsConfig resolverCredentialsConfig;
    private final boolean bypassProxy;
    private int timeout = DEFAULT_CONNECTION_TIMEOUT;
    private Integer connectionRetry;
    private Integer deploymentThreads;

    @DataBoundConstructor
    public JFrogPlatformInstance(String instanceId, String url, String artifactoryUrl, CredentialsConfig deployerCredentialsConfig,
                                 CredentialsConfig resolverCredentialsConfig, int timeout, boolean bypassProxy, Integer connectionRetry, Integer deploymentThreads) {
        this.id = instanceId;
        this.url = StringUtils.isNotEmpty(url) ? StringUtils.removeEnd(url, "/") : null;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
        this.resolverCredentialsConfig = resolverCredentialsConfig;
        this.timeout = timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT;
        this.bypassProxy = bypassProxy;
        this.connectionRetry = connectionRetry != null ? connectionRetry : 3;
        this.deploymentThreads = deploymentThreads != null && deploymentThreads > 0 ? deploymentThreads : DEFAULT_DEPLOYMENT_THREADS_NUMBER;
        artifactoryServer = new ArtifactoryServer(this.id, artifactoryUrl, this.deployerCredentialsConfig, this.resolverCredentialsConfig, this.timeout, this.bypassProxy, this.connectionRetry, this.deploymentThreads);
    }

    public JFrogPlatformInstance(ArtifactoryServer artifactoryServer) {
        this(artifactoryServer.getServerId(), "", artifactoryServer.getArtifactoryUrl(), artifactoryServer.getDeployerCredentialsConfig(), artifactoryServer.getResolverCredentialsConfig(), artifactoryServer.getTimeout(), artifactoryServer.isBypassProxy(), artifactoryServer.getConnectionRetry(), artifactoryServer.getDeploymentThreads());
    }

    public JFrogPlatformInstance(org.jfrog.hudson.pipeline.common.types.ArtifactoryServer artifactoryServer) {
        this("", "",artifactoryServer.getUrl(), artifactoryServer.createCredentialsConfig(), artifactoryServer.createCredentialsConfig(), artifactoryServer.getConnection().getTimeout(), artifactoryServer.isBypassProxy(), artifactoryServer.getConnection().getRetry(), artifactoryServer.getDeploymentThreads());
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public ArtifactoryServer getArtifactoryServer() {
        return artifactoryServer;
    }

    public CredentialsConfig getDeployerCredentialsConfig() {
        return deployerCredentialsConfig;
    }

    public CredentialsConfig getResolverCredentialsConfig() {
        return resolverCredentialsConfig;
    }


    // To populate the dropdown list from the jelly
    public List<Integer> getConnectionRetries() {
        List<Integer> items = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++) {
            items.add(i);
        }
        return items;
    }

    /**
     * Return number of deployment threads.
     * To populate the dropdown list from the jelly:
     * <j:forEach var="r" items="${server.deploymentsThreads}">
     */
    @SuppressWarnings("unused")
    public List<Integer> getDeploymentsThreads() {
        List<Integer> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            items.add(i);
        }
        return items;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isBypassProxy() {
        return bypassProxy;
    }

    public int getConnectionRetry() {
        if (connectionRetry == null) {
            connectionRetry = 3;
        }
        return connectionRetry;
    }

    public void setConnectionRetry(int connectionRetry) {
        this.connectionRetry = connectionRetry;
    }

    public Integer getDeploymentThreads() {
        return deploymentThreads;
    }

    /**
     * Set the number of deployment threads.
     * Jelly uses reflection here and 'getDeploymentsThreads()' to get the data by the method and variable (matching) names
     * <f:option selected="${r==server.deploymentThreads}"
     *
     * @param deploymentThreads - Deployment threads number
     */
    @SuppressWarnings("unused")
    public void setDeploymentThreads(int deploymentThreads) {
        this.deploymentThreads = deploymentThreads;
    }
}
