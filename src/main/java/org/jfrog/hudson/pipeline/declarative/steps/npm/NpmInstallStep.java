package org.jfrog.hudson.pipeline.declarative.steps.npm;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Run npm-install task.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class NpmInstallStep extends NpmInstallCiStepBase {
    static final String STEP_NAME = "rtNpmInstall";

    @DataBoundConstructor
    public NpmInstallStep() {
        super();
        this.ciCommand = false;
    }

    @Override
    public String getStepName() {
        return STEP_NAME;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmInstallStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory npm install";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
