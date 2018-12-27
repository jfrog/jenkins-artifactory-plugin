package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.resolvers.GradleResolver;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class GradleResolverStep extends GradleDeployerResolver {

    static final String STEP_NAME = "rtGradleResolver";

    @DataBoundConstructor
    public GradleResolverStep(String id, String serverId, String repo) {
        super(STEP_NAME, id, serverId);
        GradleResolver gradleResolver = new GradleResolver();
        gradleResolver.setRepo(repo);
        buildDataFile.putPOJO(gradleResolver);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GradleResolverStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "set gradle resolver";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
