package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class GradleDeployerStep extends GradleDeployerResolver {

    static final String STEP_NAME = "rtGradleDeployer";

    @DataBoundConstructor
    public GradleDeployerStep(String id, String repo, String serverId) {
        super(STEP_NAME, id, repo, serverId);
    }

    @DataBoundSetter
    public void setDeployMavenDescriptors(String deployMavenDescriptors) {
        buildDataFile.put("deployMavenDescriptors", deployMavenDescriptors);
    }

    @DataBoundSetter
    public void setDeployIvyDescriptors(String deployIvyDescriptors) {
        buildDataFile.put("deployIvyDescriptors", deployIvyDescriptors);
    }

    @DataBoundSetter
    public void setIvyPattern(String ivyPattern) {
        buildDataFile.put("ivyPattern", ivyPattern);
    }

    @DataBoundSetter
    public void setArtifactPattern(String artifactPattern) {
        buildDataFile.put("artifactPattern", artifactPattern);
    }

    @DataBoundSetter
    public void setMavenCompatible(String mavenCompatible) {
        buildDataFile.put("mavenCompatible", mavenCompatible);
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
