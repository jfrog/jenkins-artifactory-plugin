package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

public class CreateServerStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtServer";
    private BuildDataFile buildDataFile;

    @DataBoundConstructor
    public CreateServerStep(String id) {
        buildDataFile = new BuildDataFile(STEP_NAME, id);
    }

    @DataBoundSetter
    public void setUrl(String url) {
        buildDataFile.put("url", url);
    }

    @DataBoundSetter
    public void setUsername(String username) {
        buildDataFile.put("username", username);
    }

    @DataBoundSetter
    public void setPassword(String password) {
        buildDataFile.put("password", password);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        buildDataFile.put("credentialsId", credentialsId);
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient CreateServerStep step;

        @StepContextParameter
        private transient FilePath ws;

        @Override
        protected Void run() throws Exception {
            String buildNumber = DeclarativePipelineUtils.getBuildNumberFromStep(getContext());
            BuildDataFile buildDataFile = step.buildDataFile;
            writeBuildDataFile(ws, buildNumber, buildDataFile);
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateServerStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Creates new Artifactory server";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
