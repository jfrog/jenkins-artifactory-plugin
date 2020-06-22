package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.FilePath;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.jfpipelines.payloads.JobStartedPayload;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.steps.JfPipelinesStep;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.SerializationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    /**
     * Get 'workspace' dir for the input project.
     *
     * @param project - The project
     * @return the 'workspace' dir.
     */
    public static FilePath getWorkspace(Job<?, ?> project) {
        FilePath projectJob = new FilePath(project.getRootDir());
        return projectJob.getParent().sibling("workspace").child(project.getName());
    }

    /**
     * Get JFrog Pipelines server from the global configuration or null if not defined.
     *
     * @return configured JFrog Pipelines server.
     */
    public static JFrogPipelinesServer getPipelinesServer() {
        ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl) Jenkins.get().getDescriptor(ArtifactoryBuilder.class);
        if (descriptor == null) {
            return null;
        }
        JFrogPipelinesServer pipelinesServer = descriptor.getJfrogPipelinesServer();
        if (StringUtils.isBlank(pipelinesServer.getIntegrationUrl())) {
            return null;
        }
        return pipelinesServer;
    }

    /**
     * Inject the 'JFROG_PIPELINES_INFO' parameter to the project's parameters list.
     *
     * @param project      - The project
     * @param defaultValue - The default value - for tests
     * @throws IOException in case of any error.
     */
    public static void injectJfPipelinesInfoParameter(Job<?, ?> project, String defaultValue) throws IOException {
        ParametersDefinitionProperty parametersProperty = project.getProperty(ParametersDefinitionProperty.class);
        ParameterDefinition parameterDefinition = new JFrogPipelinesParameter(defaultValue);
        if (parametersProperty == null) {
            project.addProperty(new ParametersDefinitionProperty(parameterDefinition));
        } else if (parametersProperty.getParameterDefinition(JFrogPipelinesParameter.PARAM_NAME) == null) {
            List<ParameterDefinition> parameterDefinitions = new ArrayList<>(parametersProperty.getParameterDefinitions());
            parameterDefinitions.add(parameterDefinition);
            project.removeProperty(parametersProperty);
            project.addProperty(new ParametersDefinitionProperty(parameterDefinitions));
        }
    }

    /**
     * Extract 'JFROG_PIPELINES_INFO' parameter from build.
     *
     * @param build    - The build
     * @param listener - The task listener
     * @return JobStartedPayload or null.
     */
    public static JobStartedPayload getJobStartedPayload(Run<?, ?> build, TaskListener listener) {
        ParametersAction parametersAction = build.getAction(ParametersAction.class);
        if (parametersAction == null) {
            return null;
        }
        ParameterValue value = parametersAction.getParameter(JFrogPipelinesParameter.PARAM_NAME);
        if (value == null) {
            return null;
        }
        try {
            return SerializationUtils.createMapper().readValue((String) value.getValue(), JobStartedPayload.class);
        } catch (JsonProcessingException exception) {
            listener.error("Couldn't deserialize 'JFROG_PIPELINES_INFO' parameter", exception);
        }
        return null;
    }

    /**
     * Extract JFrog Pipelines job info from build.
     *
     * @param build - the build
     * @return JFrog Pipelines job info
     */
    public static JFrogPipelinesJobInfo getPipelinesJobInfo(Run<?, ?> build) throws IOException, InterruptedException {
        BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(getWorkspace(build.getParent()), String.valueOf(build.getNumber()), JfPipelinesStep.STEP_NAME, "0");
        if (buildDataFile == null) {
            return null;
        }
        return SerializationUtils.createMapper().treeToValue(buildDataFile.get(JfPipelinesStep.STEP_NAME), JFrogPipelinesJobInfo.class);
    }

    /**
     * Return true if the JFrog Pipelines server is not configured.
     *
     * @param pipelinesServer - The server to check
     * @return true if the JFrog Pipelines server is not configured.
     */
    public static boolean isNotConfigured(JFrogPipelinesServer pipelinesServer) {
        return pipelinesServer == null || !StringUtils.isNotBlank(pipelinesServer.getIntegrationUrl());
    }

    /**
     * Create job info map for reporting job queue.
     *
     * @param queueItem - The Jenkins queue item
     * @return Job info map.
     */
    public static Map<String, String> createJobInfo(Queue.Item queueItem) {
        return new HashMap<String, String>() {{
            put("queueId", String.valueOf(queueItem.getId()));
        }};
    }

    /**
     * Create job info map for reporting build 'started' and 'completed'.
     *
     * @param build - The build
     * @return Job info map.
     */
    public static Map<String, String> createJobInfo(Run<?, ?> build) {
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
}
