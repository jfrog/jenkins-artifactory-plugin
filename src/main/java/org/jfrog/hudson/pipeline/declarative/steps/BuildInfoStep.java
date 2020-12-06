package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Create build info.
 */
@SuppressWarnings("unused")
public class BuildInfoStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtBuildInfo";
    private final BuildInfo buildInfo;

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
    public void setIncludeEnvPatterns(List<String> includeEnvPatterns) {
        includeEnvPatterns.forEach(pattern -> buildInfo.getEnv().getFilter().addInclude(pattern));
    }

    @DataBoundSetter
    public void setExcludeEnvPatterns(List<String> excludeEnvPatterns) {
        excludeEnvPatterns.forEach(pattern -> buildInfo.getEnv().getFilter().addExclude(pattern));
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
    public void setDoNotDiscardBuilds(List<String> buildNumbersNotToBeDiscarded) {
        buildInfo.getRetention().setDoNotDiscardBuilds(buildNumbersNotToBeDiscarded);
    }

    @DataBoundSetter
    public void setMaxDays(int days) {
        buildInfo.getRetention().setMaxDays(days);
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<Void> {

        private transient final BuildInfoStep step;

        @Inject
        public Execution(BuildInfoStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            String buildName = StringUtils.isBlank(step.buildInfo.getName()) ? BuildUniqueIdentifierHelper.getBuildName(build) : step.buildInfo.getName();
            String buildNumber = StringUtils.isBlank(step.buildInfo.getNumber()) ? BuildUniqueIdentifierHelper.getBuildNumber(build) : step.buildInfo.getNumber();
            step.buildInfo.setName(buildName);
            step.buildInfo.setNumber(buildNumber);
            DeclarativePipelineUtils.saveBuildInfo(step.buildInfo, rootWs, build, new JenkinsBuildInfoLog(listener));
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
