package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesJobProperty;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
import org.jfrog.hudson.jfpipelines.OutputResource;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run<?, ?> build;

        @Inject(optional = true)
        private transient JfPipelinesStep step;

        @Override
        protected Void run() throws Exception {
            JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
            JFrogPipelinesJobProperty property = build.getParent().getProperty(JFrogPipelinesJobProperty.class);
            if (property == null) {
                logger.info("Skipping jfPipelines step.");
                return null;
            }
            String stepId = property.getPayload().getStepId();
            JFrogPipelinesServer pipelinesServer = JFrogPipelinesServer.getPipelinesServer();
            if (!JFrogPipelinesServer.isConfigured(pipelinesServer)) {
                throw new IllegalStateException(JFrogPipelinesServer.SERVER_NOT_FOUND_EXCEPTION);
            }
            if (StringUtils.isNotBlank(step.outputResources)) {
                pipelinesServer.setOutputResources(stepId, OutputResource.fromString(step.outputResources));
            }
            if (StringUtils.isNotBlank(step.reportStatus)) {
                if (!ACCEPTABLE_RESULTS.contains(StringUtils.upperCase(step.reportStatus))) {
                    throw new IllegalArgumentException("Illegal build results '" + step.reportStatus + "'. Acceptable values: " + ACCEPTABLE_RESULTS);
                }
                if (pipelinesServer.isReported(stepId)) {
                    throw new IllegalStateException("This job already reported the status to JFrog Pipelines Step ID " + stepId + ". You can run jfPipelines with the 'reportStatus' parameter only once.");
                }
                pipelinesServer.setReported(stepId);
                pipelinesServer.report(build, step.reportStatus, stepId, logger);
            }
            return null;
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
