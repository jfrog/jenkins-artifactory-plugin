package org.jfrog.hudson.jfpipelines;

import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.jfpipelines.payloads.JobStatusPayload;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
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
        this.connectionRetries = connectionRetries > 0 ? connectionRetries : DEFAULT_CONNECTION_RETRIES;
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
     * Report queue if when the Jon enters the Jenkins queue.
     *
     * @param queueItem - The queue item to report
     * @param property  - The JFrogPipelinesJobProperty contains the JFrog Pipelines step ID.
     */
    public static void reportQueueId(Queue.Item queueItem, JFrogPipelinesJobProperty property) {
        try {
            JFrogPipelinesServer pipelinesServer = getPipelinesServer();
            if (!isConfigured(pipelinesServer)) {
                // JFrog Pipelines server is not configured, but 'JFrogPipelines' parameter is set.
                throw new IOException(SERVER_NOT_FOUND_EXCEPTION);
            }
            pipelinesServer.report(BUILD_QUEUED, property, createJobInfo(queueItem), new NullLog());
        } catch (IOException e) {
            System.err.println(FAILURE_PREFIX);
            ExceptionUtils.printRootCauseStackTrace(e);
        }
    }

    /**
     * Reported 'STARTED' status to JFrog Pipelines when a Jenkins job starts running.
     *
     * @param build    - The build
     * @param listener - The task listener
     */
    public static void reportStarted(Run<?, ?> build, TaskListener listener) {
        JFrogPipelinesServer pipelinesServer = null;
        JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
        try {
            JFrogPipelinesJobProperty property = build.getParent().getProperty(JFrogPipelinesJobProperty.class);
            if (property == null) {
                return;
            }
            pipelinesServer = getPipelinesServer();
            if (!isConfigured(pipelinesServer)) {
                // JFrog Pipelines server is not configured, but 'JFrogPipelines' parameter is set.
                logger.error(SERVER_NOT_FOUND_EXCEPTION);
                return;
            }
            pipelinesServer.report(BUILD_STARTED, property, createJobInfo(build), logger);
        } catch (IOException e) {
            if (isConfigured(pipelinesServer)) {
                // If JFrog Pipelines server is not configured - don't log errors.
                // This case is feasible when removeProperty throws an exception.
                logger.error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
            }
        }
    }

    /**
     * Report status to JFrog Pipelines when a Jenkins job completes running.
     *
     * @param build    - The build
     * @param listener - The task listener
     */
    public static void reportCompleted(Run<?, ?> build, TaskListener listener) {
        JFrogPipelinesServer pipelinesServer = null;
        JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
        try {
            JFrogPipelinesJobProperty property = build.getParent().removeProperty(JFrogPipelinesJobProperty.class);
            if (property == null) {
                return;
            }
            pipelinesServer = getPipelinesServer();
            if (!isConfigured(pipelinesServer)) {
                // JFrog Pipelines server is not configured, but 'JFrogPipelines' parameter is set.
                logger.error(SERVER_NOT_FOUND_EXCEPTION);
                return;
            }
            if (property.isReported()) {
                // Step status is already reported to JFrog Pipelines.
                logger.debug("Skipping reporting to JFrog Pipelines - status is already reported in jfPipelines step.");
                return;
            }
            Result result = ObjectUtils.defaultIfNull(build.getResult(), Result.NOT_BUILT);
            pipelinesServer.report(result.toExportedObject(), property, createJobInfo(build), logger);
        } catch (IOException e) {
            if (isConfigured(pipelinesServer)) {
                // If JFrog Pipelines server is not configured - don't log errors.
                // This case is feasible when removeProperty throws an exception.
                logger.error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
            }
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
     * @param result   - The build results - on of {QUEUED, STARTED, SUCCESS, UNSTABLE, FAILURE, NOT_BUILT, ABORTED}
     * @param property - The build
     * @param jobInfo  - The job info payload
     * @param logger   - The build logger
     */
    public void report(String result, JFrogPipelinesJobProperty property, Map<String, String> jobInfo, Log logger) throws IOException {
        // Report job completed to JFrog Pipelines
        try (JFrogPipelinesHttpClient client = createHttpClient(logger)) {
            client.sendStatus(new JobStatusPayload(result, property.getPayload().getStepId(), jobInfo, OutputResource.fromString(property.getOutputResources())));
        }
        logger.info("Successfully reported status '" + result + "' to JFrog Pipelines.");
    }
}
