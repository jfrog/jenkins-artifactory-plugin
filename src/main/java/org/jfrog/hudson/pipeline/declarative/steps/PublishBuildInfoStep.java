package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.PublishBuildInfoExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

@SuppressWarnings("unused")
public class PublishBuildInfoStep extends AbstractStepImpl {
    public static final String STEP_NAME = "rtPublishBuildInfo";
    private final String serverId;
    private String buildNumber;
    private String buildName;
    private String project;

    @DataBoundConstructor
    public PublishBuildInfoStep(String serverId) {
        this.serverId = serverId;
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    @DataBoundSetter
    public void setProject(String buildProject) {
        this.project = buildProject;
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<Void> {

        private transient final PublishBuildInfoStep step;

        @Inject
        public Execution(PublishBuildInfoStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.buildName, step.buildNumber, step.project);
            if (buildInfo == null) {
                throw new RuntimeException("Build " + DeclarativePipelineUtils.createBuildInfoId(build, step.buildName, step.buildNumber, step.project) + " does not exist!");
            }
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, step.serverId, true);
            new PublishBuildInfoExecutor(build, listener, buildInfo, server, ws).execute();
            return null;
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() throws Exception {
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, step.serverId, true);
            return Utils.prepareArtifactoryServer(null, server);
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
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
