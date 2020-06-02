package org.jfrog.hudson.action;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.TransientProjectActionFactory;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.model.TransientActionFactory;
import jenkins.util.TimeDuration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesJobProperty;
import org.jfrog.hudson.jfpipelines.payloads.JobStartedPayload;
import org.jfrog.hudson.util.SerializationUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * This class is used for managing the JFrog Pipelines Job triggering functionality.
 * The API is invoked using a URL with the following pattern:
 * POST <Jenkins server>/Jenkins>/job/<Project>/jfrog/pipelines?<job-parameters>
 * {
 * "stepId": "<step-id>"
 * }
 *
 * @param <JobT> - AbstractProject or WorkflowJob
 */
@SuppressWarnings({"unused"})
public class JfrogPipelinesAction<JobT extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob<?, ?>> implements Action {

    private final JobT project;

    public JfrogPipelinesAction(JobT job) {
        this.project = job;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "JFrog Pipelines job wrapper";
    }

    @Override
    public String getUrlName() {
        return "jfrog";
    }

    /**
     * Implements the "/pipelines" endpoint.
     *
     * @param req  - The REST API request
     * @param resp - The response
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @RequirePOST
    public void doPipelines(StaplerRequest req, StaplerResponse resp) {
        try {
            JobStartedPayload payload = SerializationUtils.createMapper().readValue(req.getInputStream(), JobStartedPayload.class);
            project.addProperty(new JFrogPipelinesJobProperty(payload));
            runBuild(project, req, resp);
        } catch (IOException | ServletException e) {
            ExceptionUtils.printRootCauseStackTrace(e);
        }
    }

    /**
     * Trigger Jenkins build.
     *
     * @param job  - AbstractProject for UI jobs or WorkflowJob for Jenkins pipelines
     * @param req  - The REST API request
     * @param resp - The response
     * @throws IOException      in case of errors during starting the build
     * @throws ServletException in case of errors during starting the build
     */
    private void runBuild(JobT job, StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        TimeDuration quietPeriod = new TimeDuration(Jenkins.get().getQuietPeriod());
        if (job.isParameterized()) {
            job.doBuildWithParameters(req, resp, quietPeriod);
        } else {
            job.doBuild(req, resp, quietPeriod);
        }
    }

    /**
     * This sub-class registers JFrog Pipelines action on Jenkins pipelines jobs.
     */
    @SuppressWarnings({"unused"})
    @Extension
    public static class WorkflowActionFactory extends TransientActionFactory<WorkflowJob> {

        @Override
        public Class<WorkflowJob> type() {
            return WorkflowJob.class;
        }

        @Nonnull
        @Override
        public Collection<? extends Action> createFor(@Nonnull WorkflowJob run) {
            return Collections.singletonList(new JfrogPipelinesAction<>(run));
        }
    }

    /**
     * This sub-class registers JFrog Pipelines action on Jenkins UI based jobs.
     */
    @SuppressWarnings("unused")
    @Extension
    public static class UiJobsActionFactory extends TransientProjectActionFactory {

        @Override
        public Collection<? extends Action> createFor(AbstractProject target) {
            return Collections.singletonList(new JfrogPipelinesAction<>(target));
        }
    }
}
