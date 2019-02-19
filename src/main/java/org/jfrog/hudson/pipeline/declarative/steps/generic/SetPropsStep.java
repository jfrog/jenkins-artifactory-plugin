package org.jfrog.hudson.pipeline.declarative.steps.generic;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper.EditPropertiesActionType;

@SuppressWarnings("unused")
public class SetPropsStep extends EditPropsStep {

    @DataBoundConstructor
    public SetPropsStep(String serverId) {
        super(serverId, EditPropertiesActionType.SET);
    }

    public static class Execution extends EditPropsStep.Execution {

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected Void run() throws Exception {
            super.editPropsRun(build, listener, step, ws, env);
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(SetPropsStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtSetProps";
        }

        @Override
        public String getDisplayName() {
            return "Set properties";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
