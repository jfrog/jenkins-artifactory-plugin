package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by romang on 5/2/16.
 */
public class CreateBuildInfoStep extends AbstractStepImpl {

    @DataBoundConstructor
    public CreateBuildInfoStep() {
    }

    public static class Execution extends AbstractSynchronousStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient CreateBuildInfoStep step;

        @Override
        protected BuildInfo run() throws Exception {
            return new BuildInfo(build);
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateBuildInfoStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newBuildInfo";
        }

        @Override
        public String getDisplayName() {
            return "New buildInfo";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}

