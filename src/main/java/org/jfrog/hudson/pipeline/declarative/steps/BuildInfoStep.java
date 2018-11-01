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

public class BuildInfoStep extends AbstractStepImpl {

    public static final String STEP_NAME = "buildInfo";
    private String buildName;
    private String buildNumber;

    @DataBoundConstructor
    public BuildInfoStep() {
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
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
            String buildName = StringUtils.isBlank(step.buildName) ? DeclarativePipelineUtils.getBuildName(getContext()) : step.buildName;
            String buildNumber = StringUtils.isBlank(step.buildNumber) ? DeclarativePipelineUtils.getBuildNumber(getContext()) : step.buildNumber;
            BuildInfo buildInfo = new BuildInfo(build);
            buildInfo.setName(buildName);
            buildInfo.setNumber(buildNumber);
            DeclarativePipelineUtils.saveBuildInfo(buildInfo, ws, getContext());
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
