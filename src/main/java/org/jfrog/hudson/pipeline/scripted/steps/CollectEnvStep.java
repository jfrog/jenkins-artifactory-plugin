package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Env;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by romang on 5/2/16.
 */
public class CollectEnvStep extends AbstractStepImpl {

    private Env env;

    @DataBoundConstructor
    public CollectEnvStep(Env env) {
        this.env = env;
    }

    public Env getEnv() {
        return env;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {

        private transient CollectEnvStep step;

        @Inject
        public Execution(CollectEnvStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean run() throws Exception {
            step.getEnv().collectVariables(env, build, listener);
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CollectEnvStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "collectEnv";
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

