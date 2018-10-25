package org.jfrog.hudson.pipeline.declarative;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;

public class MavenResolveStep extends MavenDeployerResolver {

    private static final String STEP_NAME = "rtMavenResolver";

    @DataBoundConstructor
    public MavenResolveStep(String id, String releaseRepo, String snapshotRepo, String serverId) {
        super(STEP_NAME, id, releaseRepo, snapshotRepo, serverId);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(MavenResolveStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "resolve maven artifacts";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
