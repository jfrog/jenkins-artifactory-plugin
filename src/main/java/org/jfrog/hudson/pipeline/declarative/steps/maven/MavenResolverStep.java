package org.jfrog.hudson.pipeline.declarative.steps.maven;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.resolvers.MavenResolver;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class MavenResolverStep extends MavenDeployerResolver {

    static final String STEP_NAME = "rtMavenResolver";

    @DataBoundConstructor
    public MavenResolverStep(String id, String releaseRepo, String snapshotRepo, String serverId) {
        super(STEP_NAME, id, serverId);
        MavenResolver mavenDeployer = new MavenResolver();
        mavenDeployer.setReleaseRepo(releaseRepo).setSnapshotRepo(snapshotRepo);
        buildDataFile.putPOJO(mavenDeployer);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(MavenResolverStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "set maven resolver";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
