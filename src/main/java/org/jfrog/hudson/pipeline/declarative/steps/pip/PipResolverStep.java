package org.jfrog.hudson.pipeline.declarative.steps.pip;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
import org.jfrog.hudson.pipeline.declarative.steps.common.DeployerResolverBase;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Bar Belity on 08/07/2020.
 */
public class PipResolverStep extends DeployerResolverBase {

    static final String STEP_NAME = "rtPipResolver";

    @DataBoundConstructor
    public PipResolverStep(String id, String serverId, String repo) {
        super(STEP_NAME, id, serverId);
        CommonResolver pipResolver = new CommonResolver();
        pipResolver.setRepo(repo);
        buildDataFile.putPOJO(pipResolver);
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
            return "set pip resolver";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
