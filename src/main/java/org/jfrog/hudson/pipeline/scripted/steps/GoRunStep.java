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
import org.jfrog.hudson.pipeline.common.executors.GoRunExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class GoRunStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private GoBuild goBuild;
    private String path;
    private String goCmdArgs;

    @DataBoundConstructor
    public GoRunStep(BuildInfo buildInfo, GoBuild goBuild, String path, String goCmdArgs, String args) {
        this.buildInfo = buildInfo;
        this.goBuild = goBuild;
        this.path = path;
        this.goCmdArgs = goCmdArgs;
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
        private transient GoRunStep step;

        @Override
        protected BuildInfo run() throws Exception {
            GoRunExecutor goRunExecutor = new GoRunExecutor(getContext(), step.buildInfo, step.goBuild, step.path, step.goCmdArgs, ws, listener, env, build);
            goRunExecutor.execute();
            return goRunExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GoRunStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryGoRun";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory Go command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}