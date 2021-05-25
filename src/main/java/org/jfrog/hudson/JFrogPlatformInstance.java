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
    private boolean bypassProxy;
    private int timeout;
    private Integer connectionRetry;
    private Integer deploymentThreads;

    @DataBoundConstructor
    public JFrogPlatformInstance(String instanceId, String platformUrl, String artifactoryUrl, CredentialsConfig deployerCredentialsConfig,
                                 CredentialsConfig resolverCredentialsConfig, int timeout, boolean bypassProxy, Integer connectionRetry, Integer deploymentThreads) {
        this.id = instanceId;
        this.url = StringUtils.isNotEmpty(platformUrl) ? StringUtils.removeEnd(platformUrl, "/") : null;
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
        this("", "", artifactoryServer.getUrl(), artifactoryServer.createCredentialsConfig(), artifactoryServer.createCredentialsConfig(), artifactoryServer.getConnection().getTimeout(), artifactoryServer.isBypassProxy(), artifactoryServer.getConnection().getRetry(), artifactoryServer.getDeploymentThreads());
    }

    public String getId() {
        return id;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public String getInstanceId() {
        return getId();
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public void setInstanceId(String instanceId) {
        this.id = instanceId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public String getPlatformUrl() {
        return getUrl();
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public void setPlatformUrl(String url) {
        setUrl(url);
    }

    // Required by external plugins (JCasC).
    public String getArtifactoryUrl() {
        return artifactoryServer.getArtifactoryUrl();
    }

    // Required by external plugins (JCasC).
    public void setArtifactoryUrl(String artifactoryUrl) {
        artifactoryServer.setArtifactoryUrl(artifactoryUrl);
    }

    // Required by external plugins (JCasC).
    public ArtifactoryServer getArtifactory() {
        return artifactoryServer;
    }

    // Required by external plugins (JCasC).
    public CredentialsConfig getDeployerCredentialsConfig() {
        return deployerCredentialsConfig;
    }

    public void setDeployerCredentialsConfig(CredentialsConfig cred) {
        deployerCredentialsConfig = cred;
    }

    // Required by external plugins (JCasC).
    public CredentialsConfig getResolverCredentialsConfig() {
        return resolverCredentialsConfig;
    }

    // Required by external plugins (JCasC).
    public void setResolverCredentialsConfig(CredentialsConfig cred) {
        resolverCredentialsConfig = cred;
    }

    // To populate the dropdown list from the jelly
    @SuppressWarnings("unused")
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

    // Required by external plugins (JCasC).
    public int getTimeout() {
        return timeout;
    }

    // Required by external plugins (JCasC).
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public boolean isBypassProxy() {
        return getBypassProxy();
    }

    // Required by external plugins (JCasC).
    public boolean getBypassProxy() {
        return bypassProxy;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public void setBypassProxy(boolean bypassProxy) {
        this.bypassProxy = bypassProxy;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public int getConnectionRetry() {
        if (connectionRetry == null) {
            connectionRetry = 3;
        }
        return connectionRetry;
    }

    // Required by external plugins (JCasC).
    public void setConnectionRetry(int connectionRetry) {
        this.connectionRetry = connectionRetry;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
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
