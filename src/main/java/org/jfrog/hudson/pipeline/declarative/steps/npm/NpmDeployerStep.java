package org.jfrog.hudson.pipeline.declarative.steps.npm;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmGoDeployer;
import org.jfrog.hudson.pipeline.declarative.steps.common.DeployerResolverBase;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;

/**
 * @author yahavi
 */
public class NpmDeployerStep extends DeployerResolverBase {

    static final String STEP_NAME = "rtNpmDeployer";
    private NpmGoDeployer npmDeployer;

    @DataBoundConstructor
    public NpmDeployerStep(String id, String serverId, String repo) {
        super(STEP_NAME, id, serverId);
        npmDeployer = new NpmGoDeployer();
        npmDeployer.setRepo(repo);
        buildDataFile.putPOJO(npmDeployer);
    }

    @DataBoundSetter
    public void setCustomBuildName(String customBuildName) {
        npmDeployer.setCustomBuildName(customBuildName);
    }

    @DataBoundSetter
    public void setProperties(List<String> properties) {
        buildDataFile.put("properties", String.join(";", properties));
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
            return "set npm deployer";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
