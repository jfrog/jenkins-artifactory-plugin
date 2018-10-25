package org.jfrog.hudson.pipeline.declarative;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;

public class MavenDeployStep extends MavenDeployerResolver {

    private static final String STEP_NAME = "rtMavenDeployer";

    @DataBoundConstructor
    public MavenDeployStep(String id, String releaseRepo, String snapshotRepo, String serverId) {
        super(STEP_NAME, id, releaseRepo, snapshotRepo, serverId);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(MavenDeployStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "deploy maven artifacts";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
