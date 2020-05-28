package org.jfrog.hudson;

import hudson.model.Result;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.hudson.jfpipelines.JobCompletedPayload;
import org.jfrog.hudson.jfpipelines.PipelinesHttpClient;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PipelinesServer implements Serializable {
    private static final String SERVER_NOT_FOUND_EXCEPTION = "Please set JFrog Pipelines server under 'Manage Jenkins' -> 'Configure System' -> 'Pipelines server'.";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 300; // 5 Minutes
    private static final int DEFAULT_CONNECTION_RETRIES = 3;

    private final Map<String, String> outputResourcesMap = new HashMap<>();
    private final Set<String> reportedStepIds = new HashSet<>();
    private final CredentialsConfig credentialsConfig;
    private final boolean bypassProxy;
    private final int connectionRetry;
    private final String cbkUrl;
    private final int timeout;

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

    public void addOutputResources(String stepId, String outputResources) {
        outputResourcesMap.put(stepId, outputResources);
    }

    public void setReported(String stepId) {
        reportedStepIds.add(stepId);
    }

    public boolean isReported(String stepId) {
        return reportedStepIds.contains(stepId);
    }

    public void clearReported(String stepId) {
        reportedStepIds.remove(stepId);
    }

    @Nullable
    public String getOutputResource(String stepId) {
        return outputResourcesMap.get(stepId);
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

    public void jobCompleted(Result result, String stepId, JenkinsBuildInfoLog logger) throws IOException {
        try (PipelinesHttpClient client = createPipelinesHttpClient(credentialsConfig.provideCredentials(null), ProxyUtils.createProxyConfiguration())) {
            client.jobCompleted(new JobCompletedPayload(result, stepId, getOutputResource(stepId)));
        }
        logger.info("Successfully reported status '" + result + "' to JFrog pipelines.");
    }

    /**
     * Get JFrog Pipelines server from the global configuration.
     *
     * @return configured JFrog Pipelines server
     */
    public static PipelinesServer getPipelinesServer() {
        ArtifactoryBuilder.DescriptorImpl descriptor =
                (ArtifactoryBuilder.DescriptorImpl) Jenkins.get().getDescriptor(ArtifactoryBuilder.class);
        if (descriptor == null) {
            throw new IllegalStateException(SERVER_NOT_FOUND_EXCEPTION);
        }
        PipelinesServer pipelinesServer = descriptor.getPipelinesServer();
        if (StringUtils.isBlank(pipelinesServer.getCbkUrl())) {
            throw new IllegalStateException(SERVER_NOT_FOUND_EXCEPTION);
        }
        return pipelinesServer;
    }
}
