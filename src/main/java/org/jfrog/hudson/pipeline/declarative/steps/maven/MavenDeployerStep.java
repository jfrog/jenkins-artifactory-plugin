package org.jfrog.hudson.pipeline.declarative.steps.maven;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.deployers.MavenDeployer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;

@SuppressWarnings("unused")
public class MavenDeployerStep extends MavenDeployerResolver {

    static final String STEP_NAME = "rtMavenDeployer";
    private MavenDeployer mavenDeployer;

    @DataBoundConstructor
    public MavenDeployerStep(String id, String releaseRepo, String snapshotRepo, String serverId) {
        super(STEP_NAME, id, serverId);
        mavenDeployer = new MavenDeployer();
        mavenDeployer.setReleaseRepo(releaseRepo).setSnapshotRepo(snapshotRepo);
        buildDataFile.putPOJO(mavenDeployer);
    }

    @DataBoundSetter
    public void setIncludePatterns(List<String> includePatterns) {
        includePatterns.forEach(pattern -> mavenDeployer.getArtifactDeploymentPatterns().addInclude(pattern));
    }

    @DataBoundSetter
    public void setExcludePatterns(List<String> excludePatterns) {
        excludePatterns.forEach(pattern -> mavenDeployer.getArtifactDeploymentPatterns().addExclude(pattern));
    }

    @DataBoundSetter
    public void setIncludeEnvVars(boolean includeEnvVars) {
        mavenDeployer.setIncludeEnvVars(includeEnvVars);
    }

    @DataBoundSetter
    public void setCustomBuildName(String customBuildName) {
        mavenDeployer.setCustomBuildName(customBuildName);
    }

    @DataBoundSetter
    public void setProperties(List<String> properties) {
        buildDataFile.put("properties", String.join(";", properties));
    }

    @DataBoundSetter
    public void setDeployArtifacts(boolean deployArtifacts) {
        mavenDeployer.setDeployArtifacts(deployArtifacts);
    }

    @DataBoundSetter
    public void setDeployEvenIfUnstable(boolean deployEvenIfUnstable) {
        mavenDeployer.setDeployEvenIfUnstable(deployEvenIfUnstable);
    }

    @DataBoundSetter
    public void setThreads(int threads) {
        mavenDeployer.setThreads(threads);
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
