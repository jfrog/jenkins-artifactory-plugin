package org.jfrog.hudson.jfpipelines;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import org.jfrog.hudson.jfpipelines.payloads.JobStartedPayload;

/**
 * This Job property is added to the build project when running from "/jfrog/pipelines" REST API, before triggering the job.
 */
public class JFrogPipelinesJobProperty extends JobProperty<Job<?, ?>> {

    private final JobStartedPayload payload;

    public JFrogPipelinesJobProperty(JobStartedPayload payload) {
        this.payload = payload;
    }

    public JobStartedPayload getPayload() {
        return payload;
    }

    /**
     * To support Jenkins Multibranch pipelines, the job property must have a descriptor.
     */
    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

    }
}
