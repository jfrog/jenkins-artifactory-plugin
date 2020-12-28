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

import static org.apache.commons.lang3.StringUtils.*;

@SuppressWarnings("unused")
public class CreateServerStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtServer";
    private final String id;

    private Integer deploymentThreads;
    private String credentialsId;
    private Boolean bypassProxy;
    private String username;
    private String password;
    private Integer timeout;
    private Integer retry;
    private String url;


    @DataBoundConstructor
    public CreateServerStep(String id) {
        this.id = id;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    @DataBoundSetter
    public void setUsername(String username) {
        this.username = username;
    }

    @DataBoundSetter
    public void setPassword(String password) {
        this.password = password;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setBypassProxy(boolean bypassProxy) {
        this.bypassProxy = bypassProxy;
    }

    @DataBoundSetter
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @DataBoundSetter
    public void setRetry(int retry) {
        this.retry = retry;
    }

    @DataBoundSetter
    public void setDeploymentThreads(int deploymentThreads) {
        this.deploymentThreads = deploymentThreads;
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
            // Prepare Artifactory server
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, ws, step.id, false);
            if (server == null) {
                server = new ArtifactoryServer();
            }
            checkInputs(server);
            overrideServerParameters(server);

            // Store Artifactory server in the BuildDataFile
            BuildDataFile buildDataFile = new BuildDataFile(STEP_NAME, step.id);
            buildDataFile.putPOJO(server);
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            DeclarativePipelineUtils.writeBuildDataFile(rootWs, buildNumber, buildDataFile, new JenkinsBuildInfoLog(listener));
            return null;
        }

        /**
         * Validate step's inputs.
         *
         * @param server - The server to check
         * @throws IOException if there is an illegal step configuration.
         */
        private void checkInputs(ArtifactoryServer server) throws IOException {
            if (isAllBlank(server.getUrl(), step.url)) {
                throw new IOException("Server URL is missing");
            }
            if (isNotBlank(step.credentialsId)) {
                if (isNotBlank(step.username)) {
                    throw new IOException("'rtServer' step can't include both credentialsId and username");
                }
                if (isNotBlank(step.password)) {
                    throw new IOException("'rtServer' step can't include both credentialsId and password");
                }
            }
        }

        /**
         * Override Artifactory pipeline server parameter with parameters configured in this step.
         *
         * @param server - The server to update
         */
        private void overrideServerParameters(ArtifactoryServer server) {
            if (isNotBlank(step.url)) {
                server.setUrl(step.url);
            }
            if (isNotBlank(step.credentialsId)) {
                server.setCredentialsId(step.credentialsId);
            }
            if (isNotBlank(step.username)) {
                server.setUsername(step.username);
            }
            if (isNotBlank(step.password)) {
                server.setPassword(step.password);
            }
            if (step.deploymentThreads != null) {
                server.setDeploymentThreads(step.deploymentThreads);
            }
            if (step.bypassProxy != null) {
                server.setBypassProxy(step.bypassProxy);
            }
            if (step.retry != null) {
                server.getConnection().setRetry(step.retry);
            }
            if (step.timeout != null) {
                server.getConnection().setTimeout(step.timeout);
            }
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
