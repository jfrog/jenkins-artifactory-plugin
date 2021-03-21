package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.BuildAppendExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class BuildAppendStep extends AbstractStepImpl {
    static final String STEP_NAME = "buildAppend";
    private final String buildNumber;
    private final String buildName;
    private final ArtifactoryServer server;
    private final BuildInfo buildInfo;

    @DataBoundConstructor
    public BuildAppendStep(BuildInfo buildInfo, String buildName, String buildNumber, ArtifactoryServer server) {
        this.buildNumber = buildNumber;
        this.buildName = buildName;
        this.buildInfo = buildInfo;
        this.server = server;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private final transient BuildAppendStep step;

        @Inject
        public Execution(BuildAppendStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            new BuildAppendExecutor(step.server, step.buildInfo, step.buildName, step.buildNumber, build, listener).execute();
            return null;
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getArtifactoryServer() {
            return Utils.prepareArtifactoryServer(null, step.getServer());
        }

        @Override
        public String getStepName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(BuildAppendStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
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