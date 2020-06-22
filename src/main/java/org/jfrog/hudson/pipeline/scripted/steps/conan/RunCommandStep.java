package org.jfrog.hudson.pipeline.scripted.steps.conan;

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
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

public class RunCommandStep extends AbstractStepImpl {
    private String command;
    private String conanHome;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public RunCommandStep(String command, String conanHome, BuildInfo buildInfo) {
        this.command = command;
        this.conanHome = conanHome;
        this.buildInfo = buildInfo;
    }

    public String getCommand() {
        return this.command;
    }

    public String getConanHome() {
        return this.conanHome;
    }

    public BuildInfo getBuildInfo() {
        return this.buildInfo;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient RunCommandStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected BuildInfo run() throws Exception {
            ConanExecutor conanExecutor = new ConanExecutor(step.getBuildInfo(), step.getConanHome(), ws, launcher, listener, env, build);
            conanExecutor.execCommand(step.getCommand());
            return conanExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(RunCommandStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "runConanCommand";
        }

        @Override
        public String getDisplayName() {
            return "Run a Conan command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}