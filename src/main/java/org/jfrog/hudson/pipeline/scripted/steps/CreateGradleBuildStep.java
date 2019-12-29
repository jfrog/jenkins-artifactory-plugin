package org.jfrog.hudson.pipeline.scripted.steps;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.builds.GradleBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateGradleBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateGradleBuildStep() {
    }

    public static class Execution extends AbstractSynchronousStepExecution<GradleBuild> {
        private static final long serialVersionUID = 1L;

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

