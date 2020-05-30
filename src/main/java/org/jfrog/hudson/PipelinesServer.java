package org.jfrog.hudson;

import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.hudson.jfpipelines.JfrogPipelinesParam;
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
    public static final String FAILURE_PREFIX = "Failed to report status to JFrog Pipelines: ";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 300; // 5 Minutes
    private static final int DEFAULT_CONNECTION_RETRIES = 3;

    // Map between step ID and output resources.
    private final Map<String, String> outputResourcesMap = new HashMap<>();
    // Set of reported step IDs. This is important to avoid reporting status to JFrog pipelines more than once.
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

    // Populate connection retries list from the Jelly
    @SuppressWarnings("unused")
    public List<Integer> getConnectionRetries() {
        return IntStream.range(0, 10).boxed().collect(Collectors.toList());
    }

    public int getConnectionRetry() {
        return connectionRetry;
    }

    /**
     * Set the output resources of the step ID.
     *
     * @param stepId          - Step ID from JFrog Pipelines
     * @param outputResources - Output resources map to report to JFrog Pipelines
     */
    public void setOutputResources(String stepId, String outputResources) {
        outputResourcesMap.put(stepId, outputResources);
    }

    /**
     * Run after executing the pipeline step 'jfPipelines'.
     *
     * @param stepId - Step ID from JFrog Pipelines
     */
    public void setReported(String stepId) {
        reportedStepIds.add(stepId);
    }

    /**
     * Return true if the build is already reported to JFrog Pipelines.
     *
     * @param stepId - Step ID from JFrog Pipelines
     * @return true if the build is already reported to JFrog Pipelines
     */
    public boolean isReported(String stepId) {
        return reportedStepIds.contains(stepId);
    }

    /**
     * Clean up the step ID report status after build finished.
     *
     * @param stepId - Step ID from JFrog Pipelines
     */
    public void clearReported(String stepId) {
        reportedStepIds.remove(stepId);
    }

    @Nullable
    public String getOutputResource(String stepId) {
        return outputResourcesMap.get(stepId);
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

    /**
     * Send information to JFrog Pipelines after a pipeline job finished.
     *
     * @param build    - The build
     * @param listener - The task listener
     */
    @SuppressWarnings("rawtypes")
    public static void reportJob(Run build, TaskListener listener) {
        JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
        try {
            EnvVars envVars = build.getEnvironment(listener);
            JfrogPipelinesParam jfrogPipelinesParam = JfrogPipelinesParam.createFromEnv(envVars);
            if (jfrogPipelinesParam == null) {
                // JFrogPipelines parameter is not set
                return;
            }
            String stepId = jfrogPipelinesParam.getStepId();
            PipelinesServer pipelinesServer = getPipelinesServer();
            if (pipelinesServer.isReported(stepId)) {
                pipelinesServer.clearReported(stepId);
                logger.debug("Skipping reporting to JFrog Pipelines - status is already reported in jfPipelines step.");
                return;
            }
            pipelinesServer.reportNow(build.getResult(), jfrogPipelinesParam.getStepId(), logger);
        } catch (InterruptedException | IOException | IllegalArgumentException | IllegalStateException e) {
            logger.error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    /**
     * Send information to JFrog Pipelines after a pipeline job finished or when a reportNow step invoked.
     * Input parameter:
     * { stepId: <JFrog Pipelines step ID> }
     * Output:
     * {
     * action: "status",
     * status: <Jenkins build status>,
     * stepId: <JFrog Pipelines step ID>
     * }
     *
     * @param result - The build results
     * @param stepId - JFrog Pipelines step ID
     * @param logger - The build logger
     */
    public void reportNow(Result result, String stepId, JenkinsBuildInfoLog logger) throws IOException {
        try (PipelinesHttpClient client = createPipelinesHttpClient(credentialsConfig.provideCredentials(null), ProxyUtils.createProxyConfiguration(), logger)) {
            client.jobCompleted(new JobCompletedPayload(result, stepId, getOutputResource(stepId)));
        }
        logger.info("Successfully reported status '" + result + "' to JFrog Pipelines.");
    }

    /**
     * Get JFrog Pipelines server from the global configuration.
     *
     * @return configured JFrog Pipelines server
     * @throws IllegalStateException if Pipelines server is not defined
     */
    public static PipelinesServer getPipelinesServer() throws IllegalStateException {
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
