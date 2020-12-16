package org.jfrog.hudson.jfpipelines;

import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.jfpipelines.payloads.JobStartedPayload;
import org.jfrog.hudson.jfpipelines.payloads.JobStatusPayload;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.jfrog.hudson.jfpipelines.Utils.*;

public class JFrogPipelinesServer implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SERVER_NOT_FOUND_EXCEPTION = "Please configure JFrog Pipelines server under 'Manage Jenkins' -> 'Configure System' -> 'JFrog Pipelines server'.";
    public static final String FAILURE_PREFIX = "Failed to report status to JFrog Pipelines: ";
    public static final String BUILD_STARTED = "STARTED";
    public static final String BUILD_QUEUED = "QUEUED";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 300; // 5 Minutes
    private static final int DEFAULT_CONNECTION_RETRIES = 3;

    private CredentialsConfig credentialsConfig;
    private final int connectionRetries;
    private String integrationUrl;
    private boolean bypassProxy;
    private final int timeout;

    @DataBoundConstructor
    public JFrogPipelinesServer(String integrationUrl, CredentialsConfig credentialsConfig,
                                int timeout, boolean bypassProxy, int connectionRetries) {
        this.connectionRetries = connectionRetries >= 0 ? connectionRetries : DEFAULT_CONNECTION_RETRIES;
        this.integrationUrl = StringUtils.removeEnd(integrationUrl, "/");
        this.timeout = timeout > 0 ? timeout : DEFAULT_CONNECTION_TIMEOUT;
        this.credentialsConfig = credentialsConfig;
        this.bypassProxy = bypassProxy;
    }

    public JFrogPipelinesServer() {
        this.connectionRetries = DEFAULT_CONNECTION_RETRIES;
        this.timeout = DEFAULT_CONNECTION_TIMEOUT;
    }

    public String getIntegrationUrl() {
        return integrationUrl;
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
    public List<Integer> getConnectionRetriesOptions() {
        return IntStream.range(0, 10).boxed().collect(Collectors.toList());
    }

    public int getConnectionRetries() {
        return connectionRetries;
    }

    private JFrogPipelinesHttpClient createHttpClient(Log logger) {
        JFrogPipelinesHttpClient client = new JFrogPipelinesHttpClient(integrationUrl, credentialsConfig.provideCredentials(null).getAccessToken(), logger);
        client.setConnectionRetries(getConnectionRetries());
        client.setConnectionTimeout(getTimeout());
        if (!isBypassProxy()) {
            client.setProxyConfiguration(ProxyUtils.createProxyConfiguration());
        }
        return client;
    }

    /**
     * Report queue ID to JFrog Pipelines when the Job enters the Jenkins queue.
     *
     * @param queueItem - The queue item to report
     * @param stepId    - JFrog Pipelines step ID.
     * @throws IOException if the JFrog Pipelines server is not configured.
     */
    public static void reportQueueId(Queue.Item queueItem, String stepId) throws IOException {
        getAndVerifyServer().report(new JobStatusPayload(BUILD_QUEUED, stepId, createJobInfo(queueItem), null), new NullLog());
    }

    /**
     * Reported 'STARTED' status to JFrog Pipelines when a Jenkins job starts running.
     *
     * @param build    - The build
     * @param listener - The task listener
     */
    public static void reportStarted(Run<?, ?> build, TaskListener listener) {
        JobStartedPayload payload = getJobStartedPayload(build, listener);
        if (payload == null || StringUtils.isBlank(payload.getStepId())) {
            // Job is not triggered by JFrog Pipelines
            return;
        }
        JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
        try {
            getAndVerifyServer().report(new JobStatusPayload(BUILD_STARTED, payload.getStepId(), createJobInfo(build), null), logger);
        } catch (IOException e) {
            logger.error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    /**
     * Report status to JFrog Pipelines when a Jenkins job completes running.
     *
     * @param build    - The build
     * @param listener - The task listener
     */
    public static void reportCompleted(Run<?, ?> build, TaskListener listener) {
        JobStartedPayload payload = getJobStartedPayload(build, listener);
        if (payload == null || StringUtils.isBlank(payload.getStepId())) {
            // Job is not triggered by JFrog Pipelines
            return;
        }
        JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
        try {
            JFrogPipelinesServer pipelinesServer = getAndVerifyServer();
            JFrogPipelinesJobInfo jobInfo = getPipelinesJobInfo(build);
            Collection<OutputResource> outputResources = null;
            if (jobInfo != null) {
                // 'jfPipelines' step is invoked in this job.
                if (jobInfo.isReported()) {
                    // Step status is already reported to JFrog Pipelines.
                    logger.debug("Skipping reporting to JFrog Pipelines - status is already reported in jfPipelines step.");
                    return;
                }
                outputResources = OutputResource.fromString(jobInfo.getOutputResources());
            }

            Result result = ObjectUtils.defaultIfNull(build.getResult(), Result.NOT_BUILT);
            pipelinesServer.report(new JobStatusPayload(result.toExportedObject(), payload.getStepId(), createJobInfo(build), outputResources), logger);
        } catch (IOException | InterruptedException e) {
            logger.error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
        } finally {
            DeclarativePipelineUtils.deleteBuildDataDir(getWorkspace(build.getParent()), String.valueOf(build.getNumber()), logger);
        }
    }

    /**
     * Report status to JFrog Pipelines after a pipeline job finished or when a reportStatus step invoked.
     * Input parameter:
     * { stepId: <JFrog Pipelines step ID> }
     * Output:
     * {
     * action: "status",
     * status: <Jenkins build status>,
     * stepId: <JFrog Pipelines step ID>
     * jobiInfo: <Jenkins job info>
     * outputResources: <Key-Value map of properties>
     * }
     *
     * @param payload - The status payload
     * @param logger  - The build logger
     */
    public void report(JobStatusPayload payload, Log logger) throws IOException {
        try (JFrogPipelinesHttpClient client = createHttpClient(logger);
             CloseableHttpResponse response = client.sendStatus(payload)) {
            EntityUtils.consumeQuietly(response.getEntity());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error(FAILURE_PREFIX + "Received: " + response.getStatusLine());
                return;
            }
        }
        logger.info("Successfully reported status '" + payload.getStatus() + "' to JFrog Pipelines.");
    }

    /**
     * Get JFrog Pipelines server or throw exception if not configured.
     *
     * @return JFrog Pipelines server
     * @throws IOException if JFrog Pipelines server is not configured.
     */
    private static JFrogPipelinesServer getAndVerifyServer() throws IOException {
        JFrogPipelinesServer pipelinesServer = getPipelinesServer();
        if (isNotConfigured(pipelinesServer)) {
            // JFrog Pipelines server is not configured, but 'JF_PIPELINES_STEP_ID' parameter is set.
            throw new IOException(SERVER_NOT_FOUND_EXCEPTION);
        }
        return pipelinesServer;
    }
}
