package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.CollectEnvExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Env;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by romang on 5/2/16.
 */
public class CollectEnvStep extends AbstractStepImpl {
    static final String STEP_NAME = "collectEnv";
    private Env env;

    @DataBoundConstructor
    public CollectEnvStep(Env env) {
        this.env = env;
    }

    public Env getEnv() {
        return env;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient CollectEnvStep step;

        @Inject
        public Execution(CollectEnvStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            CollectEnvExecutor collectEnvExecutor = new CollectEnvExecutor(build, listener, ws, step.getEnv(), env);
            collectEnvExecutor.execute();
            return null;
        }

        @Override
        public ArtifactoryServer getArtifactoryServer() {
            return null;
        }

        @Override
        public String getStepName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CollectEnvStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Collect environment variables and system properties";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}

