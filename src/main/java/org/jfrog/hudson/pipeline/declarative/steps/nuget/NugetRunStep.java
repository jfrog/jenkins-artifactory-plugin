package org.jfrog.hudson.pipeline.declarative.steps.nuget;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;

public class NugetRunStep extends NugetRunStepBase {
    static final String STEP_NAME = "rtNugetRun";

    @DataBoundConstructor
    public NugetRunStep() {
        super();
    }

    public String getResolverStepName() {
        return NugetResolverStep.STEP_NAME;
    }

    @Override
    public String getStepName() {
        return STEP_NAME;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NugetRunStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory NuGet";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
