package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateNpmBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateNpmBuildStep() {
    }

    /**
     * We don't use additional context fields in this step execution,
     * so we extend SynchronousStepExecution directly and not ArtifactorySynchronousStepExecution
     */
    public static class Execution extends SynchronousStepExecution<NpmBuild> {
        private static final long serialVersionUID = 1L;

        @Inject
        public Execution(StepContext context) {
            super(context);
        }

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
