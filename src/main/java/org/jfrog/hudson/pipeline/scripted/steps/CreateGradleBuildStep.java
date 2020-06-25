package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.types.builds.GradleBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateGradleBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateGradleBuildStep() {
    }

    /**
     * We don't use additional context fields in this step execution,
     * so we extend SynchronousStepExecution directly and not ArtifactorySynchronousStepExecution
     */
    public static class Execution extends SynchronousStepExecution<GradleBuild> {
        private static final long serialVersionUID = 1L;

        @Inject
        public Execution(StepContext context) {
            super(context);
        }

        @Override
        protected GradleBuild run() throws Exception {
            return new GradleBuild();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateGradleBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newGradleBuild";
        }

        @Override
        public String getDisplayName() {
            return "New Artifactory gradle executor";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}

