package org.jfrog.hudson.pipeline.steps.npm;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.executors.NpmInstallExecutable;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class NpmInstallStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private String installArgs;
    private NpmBuild npmBuild;
    private String rootDir;

    @DataBoundConstructor
    public NpmInstallStep(String installArgs, NpmBuild npmBuild, String rootDir, BuildInfo buildInfo) {
        this.installArgs = installArgs;
        this.buildInfo = buildInfo;
        this.npmBuild = npmBuild;
        this.rootDir = rootDir;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient NpmInstallStep step;

        @Override
        protected BuildInfo run() throws Exception {
            return new NpmInstallExecutable(listener, env, step.buildInfo, step.installArgs, step.npmBuild, launcher, step.rootDir, ws, build).execute();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmInstallStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNpmInstall";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory npm install";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
