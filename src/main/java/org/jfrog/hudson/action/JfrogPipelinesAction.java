package org.jfrog.hudson.action;

import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.model.TransientActionFactory;
import jenkins.util.TimeDuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesParameter;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
import org.jfrog.hudson.jfpipelines.payloads.JobStartedPayload;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
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

import static org.jfrog.hudson.jfpipelines.Utils.injectJfPipelinesInfoParameter;

/**
 * This class is used for managing the JFrog Pipelines Job triggering functionality.
 * The API is invoked using a URL with the following pattern:
 * POST <Jenkins server>/Jenkins>/job/<Project>/jfrog/pipelines?JFROG_PIPELINES_INFO={"stepId":"<STEP_ID>"};<job-parameters>
 *
 * @param <JobT> - AbstractProject or WorkflowJob
 */
public class JfrogPipelinesAction<JobT extends Job<?, ?> & ParameterizedJobMixIn.ParameterizedJob<?, ?>> implements Action {

    private final JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(TaskListener.NULL);
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
     * 1. Queue job with JFrogPipelinesJobProperty that contains the JFrog Pipelines step id.
     * 2. Report the queued status if exist. Typically UI based jobs have queue id.
     *
     * @param req  - The REST API request
     * @param resp - The response
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @RequirePOST
    public void doPipelines(StaplerRequest req, StaplerResponse resp) throws IOException {
        try {
            JobStartedPayload payload = getJobStartedPayload(req);
            injectJfPipelinesInfoParameter(project, "");
            Queue.Item queueItem = runBuild(project, req, resp);
            if (queueItem != null) {
                // The queue item is null in Jenkins Pipelines jobs
                JFrogPipelinesServer.reportQueueId(queueItem, payload.getStepId());
            }
            logger.debug(String.format("Queued job '%s', stepId: '%s', queueId: '%s'",
                    project.getName(), payload.getStepId(), queueItem != null ? queueItem.getId() : "N/A"));
        } catch (Exception e) {
            logger.error(ExceptionUtils.getRootCauseMessage(e), e);
            resp.sendError(400, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Extract 'JFROG_PIPELINES_INFO' parameter from HTTP request.
     *
     * @param req - The HTTP request
     * @return JobStartedPayload.
     * @throws IOException if the parameter is missing or incorrect.
     */
    private JobStartedPayload getJobStartedPayload(StaplerRequest req) throws IOException {
        String jfPipelinesInfo = req.getParameter(JFrogPipelinesParameter.PARAM_NAME);
        if (StringUtils.isBlank(jfPipelinesInfo)) {
            throw new IOException(JFrogPipelinesParameter.PARAM_NAME + " parameter is missing");
        }
        JobStartedPayload payload = SerializationUtils.createMapper().readValue(jfPipelinesInfo, JobStartedPayload.class);
        if (StringUtils.isBlank(payload.getStepId())) {
            throw new IOException("'stepId' is missing in " + JFrogPipelinesParameter.PARAM_NAME + " parameter");
        }
        return payload;
    }

    /**
     * Trigger Jenkins build.
     *
     * @param job  - AbstractProject for UI jobs or WorkflowJob for Jenkins pipelines
     * @param req  - The REST API request
     * @param resp - The response
     * @return the queue item if the job entered the Jenkins queue or null if job started running.
     * @throws IOException      in case of errors during starting the build
     * @throws ServletException in case of errors during starting the build
     */
    private Queue.Item runBuild(JobT job, StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        TimeDuration quietPeriod = new TimeDuration(Jenkins.get().getQuietPeriod());
        job.doBuildWithParameters(req, resp, quietPeriod);
        return job.getQueueItem();
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
