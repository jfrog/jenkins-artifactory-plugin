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
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.kohsuke.stapler.DataBoundConstructor;

public class AddRemoteStep extends AbstractStepImpl {
    private String serverUrl;
    private String serverName;
    private String conanHome;
    private boolean force;
    private boolean verifySSL;

    @DataBoundConstructor
    public AddRemoteStep(String serverUrl, String serverName, String conanHome, boolean force, boolean verifySSL) {
        this.serverUrl = serverUrl;
        this.serverName = serverName;
        this.conanHome = conanHome;
        this.force = force;
        this.verifySSL = verifySSL;
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

    public boolean getForce() {
        return this.force;
    }

    public boolean getVerifySSL() {
    	return this.verifySSL;
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
            ConanExecutor conanExecutor = new ConanExecutor(step.getConanHome(), ws, launcher, listener, env, build);
            conanExecutor.execRemoteAdd(step.getServerName(), step.getServerUrl(), step.getForce(), step.getVerifySSL());
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