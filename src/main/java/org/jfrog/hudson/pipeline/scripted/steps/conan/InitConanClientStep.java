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
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.common.types.ConanClient;
import org.kohsuke.stapler.DataBoundConstructor;

public class InitConanClientStep extends AbstractStepImpl {

    private ConanClient client;

    @DataBoundConstructor
    public InitConanClientStep(ConanClient client) {
        this.client = client;
    }

    public ConanClient getClient() {
        return client;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient InitConanClientStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected Boolean run() throws Exception {
            ConanClient conanClient = getConanClient();
            ConanExecutor conanExecutor = new ConanExecutor(conanClient.getUserPath(), ws, launcher, listener, env, build);
            conanExecutor.execClientInit();
            return true;
        }

        private ConanClient getConanClient() throws Exception {
            ConanClient conanClient = step.getClient();
            conanClient.setUnixAgent(launcher.isUnix());
            FilePath conanHomeDirectory = Utils.getConanHomeDirectory(conanClient.getUserPath(), env, launcher, ws);
            conanClient.setUserPath(conanHomeDirectory.getRemote());
            return conanClient;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(InitConanClientStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "initConanClient";
        }

        @Override
        public String getDisplayName() {
            return "Create Conan Client";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}