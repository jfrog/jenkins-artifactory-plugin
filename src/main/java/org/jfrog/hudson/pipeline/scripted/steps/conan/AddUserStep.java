package org.jfrog.hudson.pipeline.scripted.steps.conan;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.util.CredentialManager;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class AddUserStep extends AbstractStepImpl {
    private ArtifactoryServer server;
    private String serverName;
    private String conanHome;

    @DataBoundConstructor
    public AddUserStep(ArtifactoryServer server, String serverName, String conanHome) {
        this.server = server;
        this.serverName = serverName;
        this.conanHome = conanHome;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public String getServerName() {
        return serverName;
    }

    public String getConanHome() {
        return conanHome;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<Boolean> {

        private transient AddUserStep step;

        @Inject
        public Execution(AddUserStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean run() throws Exception {
            org.jfrog.hudson.ArtifactoryServer artifactoryServer = Utils.prepareArtifactoryServer(null, step.getServer());
            ArtifactoryConfigurator configurator = new ArtifactoryConfigurator(artifactoryServer);
            CredentialsConfig deployerConfig = CredentialManager.getPreferredDeployer(configurator, artifactoryServer);
            String username = deployerConfig.provideCredentials(build.getParent()).getUsername();
            String password = deployerConfig.provideCredentials(build.getParent()).getPassword();
            String serverName = step.getServerName();
            ConanExecutor executor = new ConanExecutor(step.getConanHome(), ws, launcher, listener, env, build);
            executor.execUserAdd(username, password, serverName);
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(AddUserStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "conanAddUser";
        }

        @Override
        public String getDisplayName() {
            return "Add new user to Conan config";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}