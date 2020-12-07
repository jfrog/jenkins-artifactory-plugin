package org.jfrog.hudson.pipeline.declarative.steps.conan;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.ConanClient;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.CredentialManager;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public class ConanRemoteStep extends AbstractStepImpl {

    private final String clientId;
    private final String name;
    private final String serverId;
    private final String repo;
    private boolean force;
    private boolean verifySSL = true;

    @DataBoundConstructor
    public ConanRemoteStep(String clientId, String name, String serverId, String repo) {
        this.clientId = clientId;
        this.name = name;
        this.serverId = serverId;
        this.repo = repo;
    }

    @DataBoundSetter
    public void setForce(boolean force) {
        this.force = force;
    }

    @DataBoundSetter
    public void setVerifySSL(boolean verifySSL) {
        this.verifySSL = verifySSL;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getName() {
        return this.name;
    }

    public String getRepo() {
        return this.repo;
    }

    public boolean getForce() {
        return this.force;
    }

    public boolean getVerifySSL() {
        return this.verifySSL;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final ConanRemoteStep step;

        @Inject
        public Execution(ConanRemoteStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            ConanClient conanClient = DeclarativePipelineUtils.buildConanClient(step.getClientId(), buildNumber, ConanClientStep.STEP_NAME, launcher, ws, rootWs, env);
            ConanExecutor conanExecutor = new ConanExecutor(conanClient.getUserPath(), ws, launcher, listener, env, build);
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, getContext(), step.serverId);
            // Run conan add remote
            String serverUrl = Utils.buildConanRemoteUrl(server, step.getRepo());
            conanExecutor.execRemoteAdd(step.getName(), serverUrl, step.getForce(), step.getVerifySSL());
            // Run conan add user
            org.jfrog.hudson.ArtifactoryServer artifactoryServer = Utils.prepareArtifactoryServer(null, server);
            ArtifactoryConfigurator configurator = new ArtifactoryConfigurator(artifactoryServer);
            CredentialsConfig deployerConfig = CredentialManager.getPreferredDeployer(configurator, artifactoryServer);
            String username = deployerConfig.provideCredentials(build.getParent()).getUsername();
            String password = deployerConfig.provideCredentials(build.getParent()).getPassword();
            conanExecutor.execUserAdd(username, password, step.getName());
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ConanRemoteStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtConanRemote";
        }

        @Override
        public String getDisplayName() {
            return "Add new repo to Conan config";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
