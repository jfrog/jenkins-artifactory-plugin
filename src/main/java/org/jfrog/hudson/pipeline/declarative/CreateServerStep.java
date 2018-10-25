package org.jfrog.hudson.pipeline.declarative;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jfrog.hudson.pipeline.declarative.utils.Utils.createTempBuildDataFile;

public class CreateServerStep extends AbstractStepImpl {

    private static final String STEP_NAME = "rtServer";
    private JSONObject jsonObject;

    @DataBoundConstructor
    public CreateServerStep(String id, String url, String username, String password, String credentialsId) {
        jsonObject = new JSONObject();
        jsonObject.element("id", id);
        jsonObject.element("stepName", STEP_NAME);
    }

    @DataBoundSetter
    public void setUrl(String url) {
        jsonObject.element("url", url);
    }

    @DataBoundSetter
    public void setUsername(String username) {
        jsonObject.element("username", username);
    }

    @DataBoundSetter
    public void setPassword(String password) {
        jsonObject.element("password", password);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        jsonObject.element("credentialsId", credentialsId);
    }

    private JSONObject getJsonObject() {
        return jsonObject;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient CreateServerStep step;

        @StepContextParameter
        private transient FilePath ws;

        @Override
        protected Void run() throws Exception {
            String buildNumber = getContext().get(WorkflowRun.class).getId();
            JSONObject jsonObject = step.getJsonObject();
            createTempBuildDataFile(ws, buildNumber, jsonObject.getString("stepName"), jsonObject.getString("id"), jsonObject.toString());
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
