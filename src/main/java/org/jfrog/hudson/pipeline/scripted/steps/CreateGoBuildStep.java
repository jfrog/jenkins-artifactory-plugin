package org.jfrog.hudson.pipeline.scripted.steps;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateGoBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateGoBuildStep() {
    }

    public static class Execution extends AbstractSynchronousStepExecution<GoBuild> {
        private static final long serialVersionUID = 1L;

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

