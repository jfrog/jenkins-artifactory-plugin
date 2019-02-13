package org.jfrog.hudson.pipeline.scripted.steps;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.NpmBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateNpmBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateNpmBuildStep() {
    }

    public static class Execution extends AbstractSynchronousStepExecution<NpmBuild> {
        private static final long serialVersionUID = 1L;

        @Override
        protected NpmBuild run() throws Exception {
            return new NpmBuild();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateNpmBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newNpmBuild";
        }

        @Override
        public String getDisplayName() {
            return "New Artifactory npm executor";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
