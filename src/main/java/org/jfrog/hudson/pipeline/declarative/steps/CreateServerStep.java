package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.types.BuildFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

public class CreateServerStep extends AbstractStepImpl {

    static final String STEP_NAME = "rtServer";
    private BuildFile buildFile;

    @DataBoundConstructor
    public CreateServerStep(String id) {
        buildFile = new BuildFile(STEP_NAME, id);
    }

    @DataBoundSetter
    public void setUrl(String url) {
        buildFile.put("url", url);
    }

    @DataBoundSetter
    public void setUsername(String username) {
        buildFile.put("username", username);
    }

    @DataBoundSetter
    public void setPassword(String password) {
        buildFile.put("password", password);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        buildFile.put("credentialsId", credentialsId);
    }

    private BuildFile getBuildFile() {
        return buildFile;
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
            BuildFile buildFile = step.getBuildFile();
            writeBuildDataFile(ws, buildNumber, buildFile);
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
