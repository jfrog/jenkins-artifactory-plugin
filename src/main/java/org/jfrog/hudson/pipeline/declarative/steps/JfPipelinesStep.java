package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesJobInfo;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
import org.jfrog.hudson.jfpipelines.OutputResource;
import org.jfrog.hudson.jfpipelines.payloads.JobStartedPayload;
import org.jfrog.hudson.jfpipelines.payloads.JobStatusPayload;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jfrog.hudson.jfpipelines.Utils.*;

@SuppressWarnings("unused")
public class JfPipelinesStep extends AbstractStepImpl {

    public static final String STEP_NAME = "jfPipelines";
    public static final List<String> ACCEPTABLE_RESULTS;
    private String outputResources;
    private String reportStatus;

    static {
        ACCEPTABLE_RESULTS = Stream.of(Result.FAILURE, Result.SUCCESS, Result.ABORTED, Result.NOT_BUILT, Result.UNSTABLE)
                .map(Result::toString)
                .collect(Collectors.toList());
    }

    @DataBoundConstructor
    public JfPipelinesStep() {
    }

    @DataBoundSetter
    public void setOutputResources(String outputResources) {
        this.outputResources = outputResources;
    }

    @DataBoundSetter
    public void setReportStatus(String reportStatus) {
        this.reportStatus = reportStatus;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run<?, ?> build;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient JfPipelinesStep step;

        @Override
        protected Void run() throws Exception {
            JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
            JobStartedPayload payload = getJobStartedPayload(build, listener);
            if (payload == null || StringUtils.isBlank(payload.getStepId())) {
                logger.info("Skipping jfPipelines step.");
                return null;
            }
            JFrogPipelinesJobInfo jobInfo = (JFrogPipelinesJobInfo) ObjectUtils.defaultIfNull(getPipelinesJobInfo(build), new JFrogPipelinesJobInfo());
            JFrogPipelinesServer pipelinesServer = getPipelinesServer();
            if (isNotConfigured(pipelinesServer)) {
                throw new IllegalStateException(JFrogPipelinesServer.SERVER_NOT_FOUND_EXCEPTION);
            }
            boolean saveJobInfo = false;
            if (StringUtils.isNotBlank(step.outputResources)) {
                jobInfo.setOutputResources(step.outputResources);
                saveJobInfo = true;
            }
            if (StringUtils.isNotBlank(step.reportStatus)) {
                if (!ACCEPTABLE_RESULTS.contains(StringUtils.upperCase(step.reportStatus))) {
                    throw new IllegalArgumentException("Illegal build results '" + step.reportStatus + "'. Acceptable values: " + ACCEPTABLE_RESULTS);
                }
                if (jobInfo.isReported()) {
                    throw new IllegalStateException("This job already reported the status to JFrog Pipelines Step ID " + payload.getStepId() + ". You can run jfPipelines with the 'reportStatus' parameter only once.");
                }
                Collection<OutputResource> outputResources = OutputResource.fromString(jobInfo.getOutputResources());
                pipelinesServer.report(new JobStatusPayload(step.reportStatus, payload.getStepId(), createJobInfo(build), outputResources), logger);

                jobInfo.setReported();
                saveJobInfo = true;
            }
            if (saveJobInfo) {
                saveJobInfo(jobInfo, logger);
            }
            return null;
        }

        /**
         * Save job info to file system.
         *
         * @param jobInfo - The job info to save
         * @param logger  - The build logger
         * @throws Exception In case of no write permissions.
         */
        private void saveJobInfo(JFrogPipelinesJobInfo jobInfo, JenkinsBuildInfoLog logger) throws Exception {
            BuildDataFile buildDataFile = new BuildDataFile(JfPipelinesStep.STEP_NAME, "0");
            buildDataFile.putPOJO(jobInfo);
            DeclarativePipelineUtils.writeBuildDataFile(getWorkspace(build.getParent()), String.valueOf(build.getNumber()), buildDataFile, logger);
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(JfPipelinesStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Set output resources and report results for JFrog Pipelines";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
