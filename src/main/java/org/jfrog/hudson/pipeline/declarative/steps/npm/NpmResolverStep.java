package org.jfrog.hudson.pipeline.declarative.steps.npm;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
import org.jfrog.hudson.pipeline.declarative.steps.common.DeployerResolverBase;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author yahavi
 */
public class NpmResolverStep extends DeployerResolverBase {

    static final String STEP_NAME = "rtNpmResolver";

    @DataBoundConstructor
    public NpmResolverStep(String id, String serverId, String repo) {
        super(STEP_NAME, id, serverId);
        CommonResolver npmResolver = new CommonResolver();
        npmResolver.setRepo(repo);
        buildDataFile.putPOJO(npmResolver);
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
            return "set npm resolver";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
