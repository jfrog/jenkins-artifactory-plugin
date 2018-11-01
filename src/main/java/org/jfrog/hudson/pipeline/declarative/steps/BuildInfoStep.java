package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Date;

@SuppressWarnings("unused")
public class BuildInfoStep extends AbstractStepImpl {

    public static final String STEP_NAME = "buildInfo";
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public BuildInfoStep() {
        buildInfo = new BuildInfo();
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        buildInfo.setName(buildName);
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        buildInfo.setNumber(buildNumber);
    }

    @DataBoundSetter
    public void setStartDate(Date date) {
        buildInfo.setStartDate(date);
    }

    @DataBoundSetter
    public void setCapture(boolean capture) {
        buildInfo.getEnv().setCapture(capture);
    }

    @DataBoundSetter
    public void addIncludeEnv(String includePattern) {
        buildInfo.getEnv().getFilter().addInclude(includePattern);
    }

    @DataBoundSetter
    public void addExcludeEnv(String excludePattern) {
        buildInfo.getEnv().getFilter().addExclude(excludePattern);
    }

    @DataBoundSetter
    public void resetEnvFilter() {
        buildInfo.getEnv().getFilter().reset();
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient FilePath ws;

        @Inject(optional = true)
        private transient BuildInfoStep step;

        @Override
        protected Void run() throws Exception {
            String buildName = StringUtils.isBlank(step.buildInfo.getName()) ? DeclarativePipelineUtils.getBuildName(getContext()) : step.buildInfo.getName();
            String buildNumber = StringUtils.isBlank(step.buildInfo.getNumber()) ? DeclarativePipelineUtils.getBuildNumber(getContext()) : step.buildInfo.getNumber();
            step.buildInfo.setName(buildName);
            step.buildInfo.setNumber(buildNumber);
            DeclarativePipelineUtils.saveBuildInfo(step.buildInfo, ws, getContext());
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(BuildInfoStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Create build info";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
