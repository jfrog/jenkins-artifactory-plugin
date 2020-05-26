package org.jfrog.hudson;

import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.ProxyUtils;
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
    private final String cbkUrl;

    @DataBoundConstructor
    public PipelinesServer(String cbkUrl, CredentialsConfig credentialsConfig,
                           int timeout, boolean bypassProxy, int connectionRetry) {
        this.connectionRetry = connectionRetry > 0 ? connectionRetry : DEFAULT_CONNECTION_RETRIES;
        this.timeout = timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT;
        this.cbkUrl = StringUtils.removeEnd(cbkUrl, "/");
        this.credentialsConfig = credentialsConfig;
        this.bypassProxy = bypassProxy;
    }

    public String getCbkUrl() {
        return cbkUrl;
    }

    public CredentialsConfig getCredentialsConfig() {
        if (credentialsConfig == null) {
            return CredentialsConfig.EMPTY_CREDENTIALS_CONFIG;
        }
        return credentialsConfig;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isBypassProxy() {
        return bypassProxy;
    }

    // To populate the dropdown list from the jelly
    @SuppressWarnings("unused")
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
        PipelinesHttpClient pipelinesHttpClient = new PipelinesHttpClient(cbkUrl, credentials.getAccessToken(), logger);
        pipelinesHttpClient.setConnectionRetries(getConnectionRetry());
        pipelinesHttpClient.setConnectionTimeout(getTimeout());
        if (!isBypassProxy()) {
            pipelinesHttpClient.setProxyConfiguration(proxyConfiguration);
        }
        return pipelinesHttpClient;
    }

    public void jobComplete() {
        try(PipelinesHttpClient client = createPipelinesHttpClient(credentialsConfig.provideCredentials(null), ProxyUtils.createProxyConfiguration())) {

        }
    }

    public static PipelinesServer getPipelinesServer() {
        ArtifactoryBuilder.DescriptorImpl descriptor =
                (ArtifactoryBuilder.DescriptorImpl) Jenkins.get().getDescriptor(ArtifactoryBuilder.class);
        if (descriptor == null) {
            return null;
        }
        return descriptor.getPipelinesServer();
    }
}
