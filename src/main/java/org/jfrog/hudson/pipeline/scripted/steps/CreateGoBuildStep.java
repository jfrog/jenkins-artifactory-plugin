package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateGoBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateGoBuildStep() {
    }

    /**
     * We don't use additional context fields in this step execution,
     * so we extend SynchronousStepExecution directly and not ArtifactorySynchronousStepExecution
     */
    public static class Execution extends SynchronousStepExecution<GoBuild> {
        private static final long serialVersionUID = 1L;

        @Inject
        public Execution(StepContext context) {
            super(context);
        }

        @Override
        protected GoBuild run() throws Exception {
            return new GoBuild();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateGoBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newGoBuild";
        }

        @Override
        public String getDisplayName() {
            return "New Artifactory Go";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}

