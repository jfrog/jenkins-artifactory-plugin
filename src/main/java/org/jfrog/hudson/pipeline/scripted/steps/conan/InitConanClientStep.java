package org.jfrog.hudson.pipeline.scripted.steps.conan;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.ConanClient;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class InitConanClientStep extends AbstractStepImpl {

    private ConanClient client;

    @DataBoundConstructor
    public InitConanClientStep(ConanClient client) {
        this.client = client;
    }

    public ConanClient getClient() {
        return client;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<Boolean> {

        private transient InitConanClientStep step;

        @Inject
        public Execution(InitConanClientStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }


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