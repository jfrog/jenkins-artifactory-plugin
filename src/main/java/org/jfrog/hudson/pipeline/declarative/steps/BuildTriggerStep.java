package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.BuildTriggerExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class BuildTriggerStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtBuildTrigger";
    private final String serverId;
    private final String paths;
    private final String spec;

    @DataBoundConstructor
    public BuildTriggerStep(String serverId, String paths, String spec) {
        this.serverId = serverId;
        this.paths = paths;
        this.spec = spec;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final BuildTriggerStep step;

        @Inject
        public Execution(BuildTriggerStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            new BuildTriggerExecutor(build, listener, server, step.paths, step.spec).execute();
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(BuildTriggerStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Trigger Artifactory build";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
