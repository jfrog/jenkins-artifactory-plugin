package org.jfrog.hudson.jfpipelines;

import org.jfrog.hudson.jfpipelines.payloads.JobStartedPayload;

import java.io.Serializable;

/**
 * This Job action is added to the build project when running from "/jfrog/pipelines" REST API, before triggering the job.
 */
public class JFrogPipelinesJobInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private JobStartedPayload payload;
    private String outputResources;
    private boolean reported;

    /**
     * Empty constructor for serialization
     */
    @SuppressWarnings("unused")
    public JFrogPipelinesJobInfo() {
    }

    public JFrogPipelinesJobInfo(JobStartedPayload payload) {
        this.payload = payload;
    }

    public JobStartedPayload getPayload() {
        return payload;
    }

    public void setOutputResources(String outputResources) {
        this.outputResources = outputResources;
    }

    public String getOutputResources() {
        return outputResources;
    }

    public void setReported() {
        this.reported = true;
    }

    public boolean isReported() {
        return reported;
    }
}
