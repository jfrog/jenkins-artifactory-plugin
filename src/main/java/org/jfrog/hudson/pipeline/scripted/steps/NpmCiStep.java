package org.jfrog.hudson.pipeline.scripted.steps;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Bar Belity on 19/11/2020.
 */
@SuppressWarnings("unused")
public class NpmCiStep extends NpmInstallCiStepBase {

    @DataBoundConstructor
    public NpmCiStep(BuildInfo buildInfo, NpmBuild npmBuild, String javaArgs, String path, String args, String module) {
        super(buildInfo, npmBuild, javaArgs, path, args, module, true);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmCiStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNpmCi";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory npm ci";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
