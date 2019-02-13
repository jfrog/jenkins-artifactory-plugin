package org.jfrog.hudson.pipeline.scripted.steps;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.MavenBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateMavenBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateMavenBuildStep() {
    }

    public static class Execution extends AbstractSynchronousStepExecution<MavenBuild> {
        private static final long serialVersionUID = 1L;

        @Override
        protected MavenBuild run() throws Exception {
            return new MavenBuild();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateMavenBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newMavenBuild";
        }

        @Override
        public String getDisplayName() {
            return "New Artifactory maven";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}

