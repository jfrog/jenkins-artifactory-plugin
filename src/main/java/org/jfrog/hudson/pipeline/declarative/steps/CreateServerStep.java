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
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@SuppressWarnings("unused")
public class CreateServerStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtServer";
    private BuildDataFile buildDataFile;
    private ArtifactoryServer server;

    @DataBoundConstructor
    public CreateServerStep(String id) {
        buildDataFile = new BuildDataFile(STEP_NAME, id);
        server = new ArtifactoryServer();
        buildDataFile.putPOJO(server);
    }

    @DataBoundSetter
    public void setUrl(String url) {
        server.setUrl(url);
    }

    @DataBoundSetter
    public void setUsername(String username) {
        server.setUsername(username);
    }

    @DataBoundSetter
    public void setPassword(String password) {
        server.setPassword(password);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        server.setCredentialsId(credentialsId);
    }

    @DataBoundSetter
    public void setBypassProxy(boolean bypassProxy) {
        server.setBypassProxy(bypassProxy);
    }

    @DataBoundSetter
    public void setTimeout(int timeout) {
        server.getConnection().setTimeout(timeout);
    }

    @DataBoundSetter
    public void setRetry(int retry) {
        server.getConnection().setRetry(retry);
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
            DeclarativePipelineUtils.writeBuildDataFile(ws, buildNumber, step.buildDataFile);
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
