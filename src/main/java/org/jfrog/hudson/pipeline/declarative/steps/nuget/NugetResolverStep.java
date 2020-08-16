package org.jfrog.hudson.pipeline.declarative.steps.nuget;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
import org.jfrog.hudson.pipeline.declarative.steps.common.DeployerResolverBase;
import org.kohsuke.stapler.DataBoundConstructor;

public class NugetResolverStep extends DeployerResolverBase {

    static final String STEP_NAME = "rtNugetResolver";

    @DataBoundConstructor
    public NugetResolverStep(String id, String serverId, String repo) {
        super(STEP_NAME, id, serverId);
        CommonResolver nugetResolver = new CommonResolver();
        nugetResolver.setRepo(repo);
        buildDataFile.putPOJO(nugetResolver);
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
            return "set NuGet resolver";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}