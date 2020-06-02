package org.jfrog.hudson.jfpipelines.payloads;

import java.io.Serializable;

/**
 * This class represents the payload sent from JFrog Pipelines before a job started.
 */
public class JobStartedPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private String stepId;

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepId() {
        return stepId;
    }
}
