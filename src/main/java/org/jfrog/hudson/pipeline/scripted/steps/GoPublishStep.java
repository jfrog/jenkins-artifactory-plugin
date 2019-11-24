package org.jfrog.hudson.pipeline.scripted.steps;

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
import org.jfrog.hudson.pipeline.common.executors.GoPublishExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.GoBuild;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class GoPublishStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private GoBuild goBuild;
    private String path;
    private String version;

    @DataBoundConstructor
    public GoPublishStep(BuildInfo buildInfo, GoBuild goBuild, String path, String args, String version) {
        this.buildInfo = buildInfo;
        this.goBuild = goBuild;
        this.path = path;
        this.version = version;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient GoPublishStep step;

        @Override
        protected BuildInfo run() throws Exception {
            //String goPackagePath = Utils.getNpmExe(ws, listener, env, launcher, step.goBuild.getTool());
            GoPublishExecutor goPublishExecutor = new GoPublishExecutor(getContext(), step.buildInfo, step.goBuild, step.path, step.version, ws, listener, build);
            goPublishExecutor.execute();
            return null;//goPublishExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GoPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryGoPublish";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory Go publish";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}