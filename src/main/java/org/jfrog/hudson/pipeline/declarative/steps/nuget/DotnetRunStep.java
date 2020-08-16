package org.jfrog.hudson.pipeline.declarative.steps.nuget;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;

public class DotnetRunStep extends NugetRunStepBase {

    @DataBoundConstructor
    public DotnetRunStep() {
        super();
        this.nugetBuild.SetUseDotnetCli(true);
    }

    public String getResolverStepName() {
        return DotnetResolverStep.STEP_NAME;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtDotnetRun";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory .NET";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
