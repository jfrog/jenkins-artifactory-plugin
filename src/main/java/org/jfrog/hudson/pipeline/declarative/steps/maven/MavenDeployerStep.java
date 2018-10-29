package org.jfrog.hudson.pipeline.declarative.steps.maven;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class MavenDeployerStep extends MavenDeployerResolver {

    static final String STEP_NAME = "rtMavenDeployer";
    private MavenDeployer mavenDeployer;

    @DataBoundConstructor
    public MavenDeployerStep(String id, String releaseRepo, String snapshotRepo, String serverId) {
        super(STEP_NAME, id, releaseRepo, snapshotRepo, serverId);
        mavenDeployer = new MavenDeployer();
        mavenDeployer.setReleaseRepo(releaseRepo);
        mavenDeployer.setSnapshotRepo(snapshotRepo);
    }

    @DataBoundSetter
    public void setDeployEvenIfUnstable(String deployEvenIfUnstable) {
        mavenDeployer.setDeployEvenIfUnstable(deployEvenIfUnstable);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(MavenDeployerStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "set maven deployer";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
