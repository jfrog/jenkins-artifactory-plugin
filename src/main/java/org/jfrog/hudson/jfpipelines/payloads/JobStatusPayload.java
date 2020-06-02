package org.jfrog.hudson.jfpipelines.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jfrog.hudson.jfpipelines.OutputResource;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;

/**
 * This class represents the payload to send to JFrog Pipelines after a job completed.
 */
public class JobStatusPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ACTION = "status";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Collection<OutputResource> outputResources;
    private final String status;
    private final String stepId;

    public JobStatusPayload(String status, String stepId, @Nullable Collection<OutputResource> outputResources) {
        this.outputResources = outputResources;
        this.status = status;
        this.stepId = stepId;
    }

    public String getAction() {
        return ACTION;
    }

    public String getStatus() {
        return status;
    }

    public String getStepId() {
        return stepId;
    }

    @SuppressWarnings("unused")
    public Collection<OutputResource> getOutputResources() {
        return outputResources;
    }
}
