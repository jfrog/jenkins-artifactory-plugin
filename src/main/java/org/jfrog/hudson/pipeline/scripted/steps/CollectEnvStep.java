package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Env;
import org.kohsuke.stapler.DataBoundConstructor;

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

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient CollectEnvStep step;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

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

