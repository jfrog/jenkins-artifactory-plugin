package org.jfrog.hudson.jfpipelines.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jfrog.hudson.jfpipelines.OutputResource;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * This class represents the payload to send to JFrog Pipelines after a job completed.
 */
@SuppressWarnings("unused")
public class JobStatusPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ACTION = "status";

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Collection<OutputResource> outputResources;
    private final Map<String, String> jobInfo;
    private final String status;
    private final String stepId;

    public JobStatusPayload(String status, String stepId, Map<String, String> jobInfo, @Nullable Collection<OutputResource> outputResources) {
        this.outputResources = outputResources;
        this.jobInfo = jobInfo;
        this.status = status;
        this.stepId = stepId;
    }

    public Collection<OutputResource> getOutputResources() {
        return outputResources;
    }

    public Map<String, String> getJobInfo() {
        return jobInfo;
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
}
