package org.jfrog.hudson.pipeline.declarative.steps.npm;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Run npm-ci task.
 *
 * Created by Bar Belity on 19/11/2020.
 */
@SuppressWarnings("unused")
public class NpmCiStep extends NpmInstallCiStepBase {
    static final String STEP_NAME = "rtNpmCi";

    @DataBoundConstructor
    public NpmCiStep() {
        super();
        this.ciCommand = true;
    }

    @Override
    public String getStepName() {
        return STEP_NAME;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmCiStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory npm ci";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
