package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.EnvVars;

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
     * @throws JsonProcessingException if JFrogPipelines environment variable is incorrect
     */
    @JsonIgnore
    public static JfrogPipelinesParam createFromEnv(EnvVars envVars) throws JsonProcessingException {
        String jfPipelinesParam = envVars.get(JF_PIPELINES_ENV);
        if (jfPipelinesParam == null) {
            // JFrogPipelines parameter is not set
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jfPipelinesParam, JfrogPipelinesParam.class);
    }
}
