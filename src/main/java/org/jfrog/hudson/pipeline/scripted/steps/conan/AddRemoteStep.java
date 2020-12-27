package org.jfrog.hudson.pipeline.scripted.steps.conan;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

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

    public static class Execution extends ArtifactorySynchronousStepExecution<Boolean> {

        private transient AddRemoteStep step;

        @Inject
        public Execution(AddRemoteStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean runStep() throws Exception {
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