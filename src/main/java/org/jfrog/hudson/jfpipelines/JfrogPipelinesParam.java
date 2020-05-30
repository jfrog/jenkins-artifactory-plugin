package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.EnvVars;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * This class represents the 'JFrogPipelines' parameter.
 */
public class JfrogPipelinesParam implements Serializable {
    private static final String JF_PIPELINES_ENV = "JFrogPipelines";

    private String stepId;

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepId() {
        return stepId;
    }

    /**
     * Create JfrogPipelinesParam from step's environment variables.
     *
     * @param envVars - The step's environment variables
     * @return JfrogPipelinesParam or null if JFrogPipelines environment variable is not set
     * @throws IllegalArgumentException if JFrogPipelines environment variable couldn't be parsed.
     */
    @JsonIgnore
    public static JfrogPipelinesParam createFromEnv(EnvVars envVars) throws IllegalArgumentException {
        String jfPipelinesParam = envVars.get(JF_PIPELINES_ENV);
        if (StringUtils.isBlank(jfPipelinesParam)) {
            // JFrogPipelines parameter is not set
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jfPipelinesParam, JfrogPipelinesParam.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Couldn't parse 'JFrogPipelines' parameter.", exception);
        }
    }

    /**
     * Create JfrogPipelinesParam from build's environment variables.
     * @param build - the build
     * @return JfrogPipelinesParam or null if JFrogPipelines environment variable is not set
     */
    @JsonIgnore
    @SuppressWarnings("rawtypes")
    public static JfrogPipelinesParam createFromBuild(Run build, TaskListener listener) {
        EnvVars envVars = new EnvVars();

        // Try to get the EnvironmentContributingAction. This should never throw exceptions.
        EnvironmentContributingAction action = build.getAction(EnvironmentContributingAction.class);
        if (action == null) {
            // EnvironmentContributingAction is missing. Try to fetch the environment variables by running build.getEnvironment.
            try {
                envVars = build.getEnvironment(listener);
            } catch (Exception e) {
                return null;
            }
        } else {
            action.buildEnvironment(build, envVars);
        }
        return JfrogPipelinesParam.createFromEnv(envVars);
    }
}
