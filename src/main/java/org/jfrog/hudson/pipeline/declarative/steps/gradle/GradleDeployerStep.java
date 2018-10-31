package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.types.deployers.GradleDeployer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
    @SuppressWarnings("unused")
    public void setDeployMavenDescriptors(String deployMavenDescriptors) {
        gradleDeployer.setDeployMavenDescriptors(deployMavenDescriptors);
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDeployIvyDescriptors(String deployIvyDescriptors) {
        gradleDeployer.setDeployIvyDescriptors(deployIvyDescriptors);
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setIvyPattern(String ivyPattern) {
        gradleDeployer.setIvyPattern(ivyPattern);
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setArtifactPattern(String artifactPattern) {
        gradleDeployer.setArtifactPattern(artifactPattern);
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setMavenCompatible(String mavenCompatible) {
        gradleDeployer.setMavenCompatible(mavenCompatible);
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
