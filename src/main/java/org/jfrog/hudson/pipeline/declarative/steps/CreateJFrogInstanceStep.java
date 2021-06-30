package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@SuppressWarnings("unused")
public class CreateJFrogInstanceStep extends AbstractStepImpl {
    public static final String STEP_NAME = "jfrogInstance";
    private final String id;

    private Integer deploymentThreads;
    private String distributionUrl;
    private String artifactoryUrl;
    private String credentialsId;
    private Boolean bypassProxy;
    private String username;
    private String password;
    private Integer timeout;
    private Integer retry;
    private String url;

    @DataBoundConstructor
    public CreateJFrogInstanceStep(String id) {
        this.id = id;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    @DataBoundSetter
    public void setDistributionUrl(String distributionUrl) {
        this.distributionUrl = distributionUrl;
    }

    @DataBoundSetter
    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
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

        private transient final CreateJFrogInstanceStep step;

        @Inject
        public Execution(CreateJFrogInstanceStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            // Prepare Artifactory server
            JFrogPlatformInstance server = DeclarativePipelineUtils.getJFrogPlatformInstance(build, ws, step.id, false);
            if (server == null) {
                server = new JFrogPlatformInstance();
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

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() throws Exception {
            return null;
        }

        @Override
        public String getUsageReportFeatureName() {
            return null;
        }

        /**
         * Validate step's inputs.
         *
         * @param server - The server to check
         * @throws IOException if there is an illegal step configuration.
         */
        private void checkInputs(JFrogPlatformInstance server) throws IOException {
            if (isAllBlank(server.getUrl(), server.getArtifactory().getUrl(), server.getDistribution().getUrl(),
                    step.url, step.artifactoryUrl, step.distributionUrl)) {
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
         * Override JFrog instance pipeline server parameter with parameters configured in this step.
         *
         * @param server - The server to update
         */
        private void overrideServerParameters(JFrogPlatformInstance server) {
            if (isNotBlank(step.url)) {
                server.setUrl(step.url);
                server.getArtifactory().setPlatformUrl(StringUtils.removeEnd(step.url, "/"));
                server.getArtifactory().setUrl(StringUtils.appendIfMissing(step.url, "/") + "artifactory");
                server.getDistribution().setUrl(StringUtils.appendIfMissing(step.url, "/") + "distribution");
            }
            if (isNotBlank(step.artifactoryUrl)) {
                server.getArtifactory().setUrl(step.artifactoryUrl);
            }
            if (isNotBlank(step.distributionUrl)) {
                server.getDistribution().setUrl(step.distributionUrl);
            }
            if (isNotBlank(step.credentialsId)) {
                server.getArtifactory().setCredentialsId(step.credentialsId);
                server.getDistribution().setCredentialsId(step.credentialsId);
            }
            if (isNotBlank(step.username)) {
                server.getArtifactory().setUsername(step.username);
                server.getDistribution().setUsername(step.username);
            }
            if (isNotBlank(step.password)) {
                server.getArtifactory().setPassword(step.password);
                server.getDistribution().setPassword(step.password);
            }

            // The following fields does not exist in the Distribution server:
            if (step.deploymentThreads != null) {
                server.getArtifactory().setDeploymentThreads(step.deploymentThreads);
            }
            if (step.bypassProxy != null) {
                server.getArtifactory().setBypassProxy(step.bypassProxy);
            }
            if (step.retry != null) {
                server.getArtifactory().getConnection().setRetry(step.retry);
            }
            if (step.timeout != null) {
                server.getArtifactory().getConnection().setTimeout(step.timeout);
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateJFrogInstanceStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Creates new JFrog instance";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
