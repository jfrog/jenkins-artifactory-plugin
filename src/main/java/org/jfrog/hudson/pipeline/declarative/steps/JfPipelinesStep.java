package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.PipelinesServer;
import org.jfrog.hudson.jfpipelines.JfrogPipelinesParam;
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
    private String reportNow;

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
    public void setReportNow(String reportNow) {
        this.reportNow = reportNow;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient EnvVars env;

        @Inject(optional = true)
        private transient JfPipelinesStep step;

        @Override
        protected Void run() throws Exception {
            JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);
            JfrogPipelinesParam jfrogPipelinesParam = JfrogPipelinesParam.createFromEnv(env);
            if (jfrogPipelinesParam == null) {
                logger.info("'JFrogPipelines' parameter is not set. Skipping jfPipelines step.");
                return null;
            }
            String stepId = jfrogPipelinesParam.getStepId();
            PipelinesServer pipelinesServer = PipelinesServer.getPipelinesServer();
            if (!PipelinesServer.isConfigured(pipelinesServer)) {
                throw new IllegalStateException(PipelinesServer.SERVER_NOT_FOUND_EXCEPTION);
            }
            if (StringUtils.isNotBlank(step.outputResources)) {
                pipelinesServer.setOutputResources(jfrogPipelinesParam.getStepId(), step.outputResources);
            }
            if (StringUtils.isNotBlank(step.reportNow)) {
                if (!ACCEPTABLE_RESULTS.contains(StringUtils.upperCase(step.reportNow))) {
                    throw new IllegalArgumentException("Illegal build results '" + step.reportNow + "'. Acceptable values: " + ACCEPTABLE_RESULTS);
                }
                if (pipelinesServer.isReported(jfrogPipelinesParam.getStepId())) {
                    throw new IllegalStateException("Step ID " + stepId + " is already reported to JFrog Pipelines. You can run jfPipelines with 'reportNow' parameter only once.");
                }
                pipelinesServer.setReported(stepId);
                pipelinesServer.reportNow(Result.fromString(step.reportNow), jfrogPipelinesParam.getStepId(), logger);
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
