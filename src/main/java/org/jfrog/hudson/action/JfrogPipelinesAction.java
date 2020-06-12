package org.jfrog.hudson.action;

import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.model.TransientActionFactory;
import jenkins.util.TimeDuration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesJobProperty;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
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

import static org.jfrog.hudson.jfpipelines.Utils.getWorkspace;

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
     * 1. Queue job with JFrogPipelinesJobProperty that contains the JFrog Pipelines step id.
     * 2. Report the queued status if exist. Typically UI based jobs have queue id.
     *
     * @param req  - The REST API request
     * @param resp - The response
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @RequirePOST
    public void doPipelines(StaplerRequest req, StaplerResponse resp) {
        try {
            JobStartedPayload payload = SerializationUtils.createMapper().readValue(req.getInputStream(), JobStartedPayload.class);
            JFrogPipelinesJobProperty property = new JFrogPipelinesJobProperty(payload);
            project.addProperty(property);
            Queue.Item queueItem = runBuild(project, req, resp);
            if (queueItem != null) {
                JFrogPipelinesServer.reportQueueId(queueItem, property);
            }
        } catch (IOException | ServletException e) {
            ExceptionUtils.printRootCauseStackTrace(e);
        }
    }

    /**
     * Save job info to fs.
     *
     * @param project - The Jenkins project
     * @param jobInfo - The job info to save
     * @param logger  - The logger
     * @throws Exception In case of no write permissions.
     */
    public static void saveJobInfo(Job<?, ?> project, JFrogPipelinesJobInfo jobInfo, Log logger) throws Exception {
        String buildNumber = String.valueOf(project.getNextBuildNumber());
        BuildDataFile buildDataFile = new BuildDataFile(JfPipelinesStep.STEP_NAME, "0");
        buildDataFile.putPOJO(jobInfo);
        DeclarativePipelineUtils.writeBuildDataFile(getWorkspace(project), buildNumber, buildDataFile, logger);
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
        if (job.isParameterized()) {
            job.doBuildWithParameters(req, resp, quietPeriod);
        } else {
            job.doBuild(req, resp, quietPeriod);
        }
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
