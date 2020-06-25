package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.types.builds.MavenBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateMavenBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateMavenBuildStep() {
    }

    /**
     * We don't use additional context fields in this step execution,
     * so we extend SynchronousStepExecution directly and not ArtifactorySynchronousStepExecution
     */
    public static class Execution extends SynchronousStepExecution<MavenBuild> {
        private static final long serialVersionUID = 1L;

        @Inject
        public Execution(StepContext context) {
            super(context);
        }

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

