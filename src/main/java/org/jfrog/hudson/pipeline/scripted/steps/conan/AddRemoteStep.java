package org.jfrog.hudson.pipeline.scripted.steps.conan;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.kohsuke.stapler.DataBoundConstructor;

public class AddRemoteStep extends AbstractStepImpl {
    private String serverUrl;
    private String serverName;
    private String conanHome;

    @DataBoundConstructor
    public AddRemoteStep(String serverUrl, String serverName, String conanHome) {
        this.serverUrl = serverUrl;
        this.serverName = serverName;
        this.conanHome = conanHome;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getConanHome() {
        return this.conanHome;
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
        private transient AddRemoteStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected Boolean run() throws Exception {
            ArgumentListBuilder args = new ArgumentListBuilder();
            args.addTokenized("conan remote add");
            args.add(step.getServerName());
            args.add(step.getServerUrl());
            EnvVars extendedEnv = new EnvVars(env);
            extendedEnv.put(Utils.CONAN_USER_HOME, step.getConanHome());
            Utils.exeConan(args, ws, launcher, listener, build, extendedEnv);
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(AddRemoteStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "conanAddRemote";
        }

        @Override
        public String getDisplayName() {
            return "Add new repo to Conan config";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}