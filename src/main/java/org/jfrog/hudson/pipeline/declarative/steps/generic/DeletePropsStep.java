package org.jfrog.hudson.pipeline.declarative.steps.generic;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper.EditPropertiesActionType;

@SuppressWarnings("unused")
public class DeletePropsStep extends EditPropsStep {

    @DataBoundConstructor
    public DeletePropsStep(String serverId) {
        super(serverId, EditPropertiesActionType.DELETE);
    }

    public static class Execution extends EditPropsStep.Execution {

        @Inject
        public Execution(EditPropsStep step, StepContext context) throws IOException, InterruptedException {
            super(step, context);
        }

        @Override
        protected Void run() throws Exception {
            super.editPropsRun(build, listener, step, ws, env);
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DeletePropsStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtDeleteProps";
        }

        @Override
        public String getDisplayName() {
            return "Delete properties";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
