package org.jfrog.hudson.pipeline.declarative.steps.go;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.declarative.steps.common.NpmGoDeployerResolver;
import org.jfrog.hudson.pipeline.common.types.resolvers.NpmGoResolver;
import org.kohsuke.stapler.DataBoundConstructor;

public class GoResolverStep extends NpmGoDeployerResolver {

    static final String STEP_NAME = "rtGoResolver";

    @DataBoundConstructor
    public GoResolverStep(String id, String serverId, String repo) {
        super(STEP_NAME, id, serverId);
        NpmGoResolver goResolver = new NpmGoResolver();
        goResolver.setRepo(repo);
        buildDataFile.putPOJO(goResolver);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "set Go resolver";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
