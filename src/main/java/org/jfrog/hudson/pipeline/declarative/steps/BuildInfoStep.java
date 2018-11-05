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
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Date;
import java.util.List;

/**
 * Create build info.
 */
@SuppressWarnings("unused")
public class BuildInfoStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtBuildInfo";
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
    public void setCaptureEnv(boolean capture) {
        buildInfo.getEnv().setCapture(capture);
    }

    @DataBoundSetter
    public void setIncludeEnv(String includePattern) {
        buildInfo.getEnv().getFilter().addInclude(includePattern);
    }

    @DataBoundSetter
    public void setExcludeEnv(String excludePattern) {
        buildInfo.getEnv().getFilter().addExclude(excludePattern);
    }

    @DataBoundSetter
    public void setMaxBuilds(int maxBuilds) {
        buildInfo.getRetention().setMaxBuilds(maxBuilds);
    }

    @DataBoundSetter
    public void setAsyncBuildRetention(boolean async) {
        buildInfo.getRetention().setAsync(async);
    }

    @DataBoundSetter
    public void setDeleteBuildArtifacts(boolean deleteBuildArtifact) {
        buildInfo.getRetention().setDeleteBuildArtifacts(deleteBuildArtifact);
    }

    @DataBoundSetter
    public void setMaxBuilds(List<String> buildNumbersNotToBeDiscarded) {
        buildInfo.getRetention().setDoNotDiscardBuilds(buildNumbersNotToBeDiscarded);
    }

    @DataBoundSetter
    public void setMaxDays(int days) {
        buildInfo.getRetention().setMaxDays(days);
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
            String buildName = StringUtils.isBlank(step.buildInfo.getName()) ? BuildUniqueIdentifierHelper.getBuildName(build) : step.buildInfo.getName();
            String buildNumber = StringUtils.isBlank(step.buildInfo.getNumber()) ? BuildUniqueIdentifierHelper.getBuildNumber(build) : step.buildInfo.getNumber();
            step.buildInfo.setName(buildName);
            step.buildInfo.setNumber(buildNumber);
            DeclarativePipelineUtils.saveBuildInfo(step.buildInfo, ws, build);
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
