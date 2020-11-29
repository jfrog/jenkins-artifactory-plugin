package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.executors.BuildAppendExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

@SuppressWarnings("unused")
public class BuildAppendStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtBuildAppend";
    private final String appendBuildNumber;
    private final String appendBuildName;
    private final String serverId;
    private String buildNumber;
    private String buildName;

    @DataBoundConstructor
    public BuildAppendStep(String serverId, String appendBuildName, String appendBuildNumber) {
        this.serverId = serverId;
        this.appendBuildName = appendBuildName;
        this.appendBuildNumber = appendBuildNumber;
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<Void> {

        private final transient BuildAppendStep step;

        @Inject
        public Execution(BuildAppendStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.buildName, step.buildNumber);
            if (buildInfo == null) {
                throw new RuntimeException("Build " + DeclarativePipelineUtils.createBuildInfoId(build, step.buildName, step.buildNumber) + " does not exist!");
            }

            new BuildAppendExecutor(server, buildInfo, step.appendBuildName, step.appendBuildNumber, build, listener).execute();
            DeclarativePipelineUtils.saveBuildInfo(buildInfo, ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(BuildAppendStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Build append";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
