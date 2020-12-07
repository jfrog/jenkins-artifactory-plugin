package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

@SuppressWarnings("unused")
public class CreateServerStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtServer";
    private final BuildDataFile buildDataFile;
    private final ArtifactoryServer server;

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

    public static class Execution extends ArtifactorySynchronousStepExecution<Void> {

        private transient final CreateServerStep step;

        @Inject
        public Execution(CreateServerStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            DeclarativePipelineUtils.writeBuildDataFile(rootWs, buildNumber, step.buildDataFile, new JenkinsBuildInfoLog(listener));
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
