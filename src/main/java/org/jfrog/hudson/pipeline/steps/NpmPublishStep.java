package org.jfrog.hudson.pipeline.steps;

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
import org.jfrog.hudson.pipeline.executors.NpmPublishExecutor;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

public class NpmPublishStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private String args;
    private NpmBuild npmBuild;
    private String rootDir;

    @DataBoundConstructor
    public NpmPublishStep(String args, NpmBuild npmBuild, String rootDir, BuildInfo buildInfo) {
        this.args = args;
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
        private transient NpmPublishStep step;

        @Override
        protected BuildInfo run() throws Exception {
            return new NpmPublishExecutor(listener, getContext(), env, step.buildInfo, step.args, step.npmBuild, launcher, step.rootDir, ws, build).execute();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNpmPublish";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory npm publish";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}