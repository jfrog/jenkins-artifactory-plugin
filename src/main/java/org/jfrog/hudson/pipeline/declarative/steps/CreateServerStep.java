package org.jfrog.hudson.pipeline.declarative.steps;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

public class CreateServerStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtServer";
    private ObjectNode jsonObject;

    @DataBoundConstructor
    public CreateServerStep(String id, String url, String username, String password, String credentialsId) {
        jsonObject = Utils.mapper().createObjectNode();
        jsonObject.put("stepName", STEP_NAME).
                put("id", id).
                put("url", url).
                put("username", username).
                put("password", password).
                put("credentialsId", credentialsId);
    }

    @DataBoundSetter
    public void setUrl(String url) {
        jsonObject.put("url", url);
    }

    @DataBoundSetter
    public void setUsername(String username) {
        jsonObject.put("username", username);
    }

    @DataBoundSetter
    public void setPassword(String password) {
        jsonObject.put("password", password);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        jsonObject.put("credentialsId", credentialsId);
    }

    private ObjectNode getJsonObject() {
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
            String buildNumber = DeclarativePipelineUtils.getBuildNumber(getContext());
            ObjectNode jsonObject = step.getJsonObject();
            writeBuildDataFile(ws, buildNumber, jsonObject.get("stepName").asText(), jsonObject.get("id").asText(), jsonObject.toString());
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
