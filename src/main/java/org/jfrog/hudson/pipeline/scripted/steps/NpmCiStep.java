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
    static final String STEP_NAME = "artifactoryNpmCi";

    @DataBoundConstructor
    public NpmCiStep(BuildInfo buildInfo, NpmBuild npmBuild, String javaArgs, String path, String args, String module) {
        super(buildInfo, npmBuild, javaArgs, path, args, module, true);
    }

    @Override
    public String getUsageReportFeatureName() {
        return STEP_NAME;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmCiStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
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
