package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.deployers.GradleDeployer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.List;

@SuppressWarnings("unused")
public class GradleDeployerStep extends GradleDeployerResolver {

    static final String STEP_NAME = "rtGradleDeployer";
    private GradleDeployer gradleDeployer;

    @DataBoundConstructor
    public GradleDeployerStep(String id, String serverId, String repo) {
        super(STEP_NAME, id, serverId);
        gradleDeployer = new GradleDeployer();
        gradleDeployer.setRepo(repo);
        buildDataFile.putPOJO(gradleDeployer);
    }

    @DataBoundSetter
    public void setIncludePatterns(List<String> includePatterns) {
        includePatterns.forEach(pattern -> gradleDeployer.getArtifactDeploymentPatterns().addInclude(pattern));
    }

    @DataBoundSetter
    public void setExcludePatterns(List<String> excludePatterns) {
        excludePatterns.forEach(pattern -> gradleDeployer.getArtifactDeploymentPatterns().addExclude(pattern));
    }

    @DataBoundSetter
    public void setIncludeEnvVars(boolean includeEnvVars) {
        gradleDeployer.setIncludeEnvVars(includeEnvVars);
    }

    @DataBoundSetter
    public void setCustomBuildName(String customBuildName) {
        gradleDeployer.setCustomBuildName(customBuildName);
    }

    @DataBoundSetter
    public void setDeployMavenDescriptors(boolean deployMavenDescriptors) {
        gradleDeployer.setDeployMavenDescriptors(deployMavenDescriptors);
    }

    @DataBoundSetter
    public void setDeployIvyDescriptors(boolean deployIvyDescriptors) {
        gradleDeployer.setDeployIvyDescriptors(deployIvyDescriptors);
    }

    @DataBoundSetter
    public void setIvyPattern(String ivyPattern) {
        gradleDeployer.setIvyPattern(ivyPattern);
    }

    @DataBoundSetter
    public void setArtifactPattern(String artifactPattern) {
        gradleDeployer.setArtifactPattern(artifactPattern);
    }

    @DataBoundSetter
    public void setMavenCompatible(boolean mavenCompatible) {
        gradleDeployer.setMavenCompatible(mavenCompatible);
    }

    @DataBoundSetter
    public void setProperties(List<String> properties) {
        buildDataFile.put("properties", String.join(";", properties));
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GradleDeployerStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "set gradle deployer";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
