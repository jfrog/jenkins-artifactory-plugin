package org.jfrog.hudson.jfpipelines;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.jfpipelines.payloads.JobStatusPayload;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JFrogPipelinesServer implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SERVER_NOT_FOUND_EXCEPTION = "Please configure JFrog Pipelines server under 'Manage Jenkins' -> 'Configure System' -> 'JFrog Pipelines server'.";
    public static final String FAILURE_PREFIX = "Failed to report status to JFrog Pipelines: ";
    public static final String BUILD_STARTED = "STARTED";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 300; // 5 Minutes
    private static final int DEFAULT_CONNECTION_RETRIES = 3;

    // Map between step ID and output resources.
    private final transient Multimap<String, OutputResource> outputResourcesMap = Multimaps.synchronizedMultimap(HashMultimap.create());
    // Set of reported step IDs. This is important to avoid reporting status to JFrog pipelines more than once.
    private final transient Set<String> reportedStepIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

    /**
     * Set the output resources of the step ID.
     *
     * @param stepId          - Step ID from JFrog Pipelines
     * @param outputResources - Output resources map to report to JFrog Pipelines
     */
    public void setOutputResources(String stepId, Collection<OutputResource> outputResources) {
        outputResourcesMap.putAll(stepId, outputResources);
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
    public Collection<OutputResource> getOutputResource(String stepId) {
        return outputResourcesMap.get(stepId);
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
     * Send information to JFrog Pipelines what a Jenkins pipeline job started.
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
            pipelinesServer.report(build, BUILD_STARTED, property.getPayload().getStepId(), logger);
        } catch (IOException e) {
            if (isConfigured(pipelinesServer)) {
                // If JFrog Pipelines server is not configured - don't log errors.
                // This case is feasible when removeProperty throws an exception.
                logger.error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
            }
        }
    }

    /**
     * Send information to JFrog Pipelines after a Jenkins pipeline job finished.
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
            String stepId = property.getPayload().getStepId();
            if (pipelinesServer.isReported(stepId)) {
                // Step status is already reported to JFrog Pipelines.
                pipelinesServer.clearReported(stepId);
                logger.debug("Skipping reporting to JFrog Pipelines - status is already reported in jfPipelines step.");
                return;
            }
            Result result = ObjectUtils.defaultIfNull(build.getResult(), Result.NOT_BUILT);
            pipelinesServer.report(build, result.toExportedObject(), property.getPayload().getStepId(), logger);
        } catch (IOException e) {
            if (isConfigured(pipelinesServer)) {
                // If JFrog Pipelines server is not configured - don't log errors.
                // This case is feasible when removeProperty throws an exception.
                logger.error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
            }
        }
    }

    private static Map<String, String> createJenkinsJobInfo(Run<?, ?> build) {
        Cause.UserIdCause cause = build.getCause(Cause.UserIdCause.class);
        return new HashMap<String, String>() {{
            put("job-name", build.getParent().getName());
            put("job-number", String.valueOf(build.getNumber()));
            put("start-time", String.valueOf(build.getStartTimeInMillis()));
            if (build.getDuration() > 0) {
                put("duration", String.valueOf(build.getDuration()));
            }
            put("build-url", build.getParent().getAbsoluteUrl() + build.getNumber());
            if (cause != null) {
                put("user", cause.getUserId());
            }
        }};
    }

    /**
     * Send information to JFrog Pipelines after a pipeline job finished or when a reportStatus step invoked.
     * Input parameter:
     * { stepId: <JFrog Pipelines step ID> }
     * Output:
     * {
     * action: "status",
     * status: <Jenkins build status>,
     * stepId: <JFrog Pipelines step ID>
     * }
     *
     * @param build  - The build
     * @param stepId - JFrog Pipelines step ID
     * @param logger - The build logger
     */
    public void report(Run<?, ?> build, String result, String stepId, JenkinsBuildInfoLog logger) throws IOException {
        // Create output resources to add to payload
        List<OutputResource> outputResource = new ArrayList<OutputResource>() {{
            Collection<OutputResource> currentResources = getOutputResource(stepId);
            if (currentResources != null) {
                addAll(currentResources);
            }
        }};

        // Report job completed to JFrog Pipelines
        try (JFrogPipelinesHttpClient client = createHttpClient(logger)) {
            client.sendStatus(new JobStatusPayload(result, stepId, createJenkinsJobInfo(build), outputResource));
        }
        logger.info("Successfully reported status '" + result + "' to JFrog Pipelines.");
    }

    /**
     * Get JFrog Pipelines server from the global configuration or null if not defined.
     *
     * @return configured JFrog Pipelines server
     */
    public static JFrogPipelinesServer getPipelinesServer() {
        ArtifactoryBuilder.DescriptorImpl descriptor =
                (ArtifactoryBuilder.DescriptorImpl) Jenkins.get().getDescriptor(ArtifactoryBuilder.class);
        if (descriptor == null) {
            return null;
        }
        JFrogPipelinesServer pipelinesServer = descriptor.getJfrogPipelinesServer();
        if (StringUtils.isBlank(pipelinesServer.getIntegrationUrl())) {
            return null;
        }
        return pipelinesServer;
    }

    public static boolean isConfigured(JFrogPipelinesServer pipelinesServer) {
        return pipelinesServer != null && StringUtils.isNotBlank(pipelinesServer.getIntegrationUrl());
    }
}
