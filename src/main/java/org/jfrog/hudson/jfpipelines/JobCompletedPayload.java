package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * This class represents the payload to send to JFrog Pipelines after a job completed.
 */
@SuppressWarnings("unused")
public class JobCompletedPayload implements Serializable {
    private static final String ACTION = "status";
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String outputResources;
    private final Result status;
    private final String stepId;

    public JobCompletedPayload(Result status, String stepId, @Nullable String outputResources) {
        this.outputResources = StringUtils.stripToNull(outputResources);
        this.status = status;
        this.stepId = stepId;
    }

    public String getAction() {
        return ACTION;
    }

    public String getStatus() {
        return status.toString();
    }

    public String getStepId() {
        return stepId;
    }

    @JsonRawValue
    public String getOutputResources() {
        return outputResources;
    }
}
