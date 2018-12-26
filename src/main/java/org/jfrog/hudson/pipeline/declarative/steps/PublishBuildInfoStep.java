package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.executors.PublishBuildInfoExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@SuppressWarnings("unused")
public class PublishBuildInfoStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtPublishBuildInfo";
    private String buildNumber;
    private String buildName;
    private String serverId;

    @DataBoundConstructor
    public PublishBuildInfoStep(String serverId) {
        this.serverId = serverId;
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

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient PublishBuildInfoStep step;

        @Override
        protected Void run() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.buildName, step.buildNumber);
            if (buildInfo == null) {
                throw new RuntimeException("Build " + DeclarativePipelineUtils.createBuildInfoId(build, step.buildName, step.buildNumber) + " does not exist!");
            }
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            new PublishBuildInfoExecutor(build, listener, buildInfo, server).execute();
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(PublishBuildInfoStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Publish build info";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
