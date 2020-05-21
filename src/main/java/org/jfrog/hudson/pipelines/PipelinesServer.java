package org.jfrog.hudson.pipelines;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.util.Credentials;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PipelinesServer implements Serializable {
    private static final int DEFAULT_CONNECTION_TIMEOUT = 300; // 5 Minutes
    private static final int DEFAULT_CONNECTION_RETRIES = 3;

    private final CredentialsConfig credentialsConfig;
    private final boolean bypassProxy;
    private final int connectionRetry;
    private final int timeout;
    private final String url;

    @DataBoundConstructor
    public PipelinesServer(String pipelinesUrl, CredentialsConfig credentialsConfig,
                           int timeout, boolean bypassProxy, int connectionRetry) {
        this.connectionRetry = connectionRetry > 0 ? connectionRetry : DEFAULT_CONNECTION_RETRIES;
        this.timeout = timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT;
        this.url = StringUtils.removeEnd(pipelinesUrl, "/");
        this.credentialsConfig = credentialsConfig;
        this.bypassProxy = bypassProxy;
    }

    public String getUrl() {
        return url;
    }

    public CredentialsConfig getCredentialsConfig() {
        return credentialsConfig;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isBypassProxy() {
        return bypassProxy;
    }

    // To populate the dropdown list from the jelly
    public List<Integer> getConnectionRetries() {
        return IntStream.range(0, 10).boxed().collect(Collectors.toList());
    }

    public int getConnectionRetry() {
        return connectionRetry;
    }

    /**
     * This method might run on slaves, this is why we provide it with a proxy from the master config
     */
    public PipelinesHttpClient createPipelinesHttpClient(Credentials credentials, ProxyConfiguration proxyConfiguration) {
        return createPipelinesHttpClient(credentials, proxyConfiguration, new NullLog());
    }

    public PipelinesHttpClient createPipelinesHttpClient(Credentials credentials, ProxyConfiguration proxyConfiguration, Log logger) {
        PipelinesHttpClient pipelinesHttpClient = new PipelinesHttpClient(url, credentials.getAccessToken(), logger);
        pipelinesHttpClient.setConnectionRetries(getConnectionRetry());
        pipelinesHttpClient.setConnectionTimeout(getTimeout());
        if (bypassProxy) {
            pipelinesHttpClient.setProxyConfiguration(proxyConfiguration.host, proxyConfiguration.port, proxyConfiguration.username, proxyConfiguration.password);
        }
        return pipelinesHttpClient;
    }
}
