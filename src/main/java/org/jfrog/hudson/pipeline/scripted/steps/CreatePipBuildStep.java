package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.builds.PipBuild;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Bar Belity on 12/07/2020.
 */
public class CreatePipBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreatePipBuildStep() {
    }

    /**
     * We don't use additional context fields in this step execution,
     * so we extend SynchronousStepExecution directly and not ArtifactorySynchronousStepExecution
     */
    public static class Execution extends SynchronousStepExecution<PipBuild> {
        private static final long serialVersionUID = 1L;

        @Inject
        public Execution(StepContext context) {
            super(context);
        }

        @Override
        protected PipBuild run() throws Exception {
            return new PipBuild();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreatePipBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newPipBuild";
        }

        @Override
        public String getDisplayName() {
            return "New Artifactory pip executor";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
