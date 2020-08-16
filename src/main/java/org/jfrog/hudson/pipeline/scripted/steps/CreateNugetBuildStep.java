package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.builds.NugetBuild;
import org.kohsuke.stapler.DataBoundConstructor;

public class CreateNugetBuildStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateNugetBuildStep() {
    }

    /**
     * We don't use additional context fields in this step execution,
     * so we extend SynchronousStepExecution directly and not ArtifactorySynchronousStepExecution
     */
    public static class Execution extends SynchronousStepExecution<NugetBuild> {
        private static final long serialVersionUID = 1L;

        @Inject
        public Execution(StepContext context) {
            super(context);
        }

        @Override
        protected NugetBuild run() throws Exception {
            return new NugetBuild();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateNugetBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newNugetBuild";
        }

        @Override
        public String getDisplayName() {
            return "New Artifactory NuGet executor";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
