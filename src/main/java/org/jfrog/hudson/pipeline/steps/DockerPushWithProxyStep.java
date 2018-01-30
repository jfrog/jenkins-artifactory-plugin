package org.jfrog.hudson.pipeline.steps;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.cli.MissingArgumentException;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.docker.proxy.BuildInfoProxy;
import org.jfrog.hudson.pipeline.docker.utils.DockerAgentUtils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by romang on 5/2/16.
 */
public class DockerPushWithProxyStep extends AbstractStepImpl {

    private final String image;
    private CredentialsConfig credentialsConfig;
    private String host;
    private BuildInfo buildInfo;
    private String targetRepo;
    // Properties to attach to the deployed docker layers.
    private ArrayListMultimap<String, String> properties;

    @DataBoundConstructor
    public DockerPushWithProxyStep(String image, CredentialsConfig credentialsConfig, String host, String targetRepo,
                                   BuildInfo buildInfo, ArrayListMultimap<String, String> properties) {

        this.image = image;
        this.credentialsConfig = credentialsConfig;
        this.host = host;
        this.targetRepo = targetRepo;
        this.buildInfo = buildInfo;
        this.properties = properties;
    }

    public String getImage() {
        return image;
    }

    public ArrayListMultimap<String, String> getProperties() {
        return properties;
    }

    public CredentialsConfig getCredentialsConfig() {
        return credentialsConfig;
    }

    public String getHost() {
        return host;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient DockerPushWithProxyStep step;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient FilePath ws;

        @Override
        protected BuildInfo run() throws Exception {
            if (!BuildInfoProxy.isUp()) {
                getContext().onFailure(new RuntimeException("Build info capturing for Docker images is not available while Artifactory proxy is not running, enable the proxy in Jenkins configuration."));
                return null;
            }

            if (step.getImage() == null) {
                getContext().onFailure(new MissingArgumentException("Missing 'image' parameter"));
                return null;
            }

            if (step.getTargetRepo() == null) {
                getContext().onFailure(new MissingArgumentException("Missing 'targetRepo' parameter"));
                return null;
            }

            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            DockerAgentUtils.registerImagOnAgents(
                    launcher, step.getImage(), step.getHost(), step.getTargetRepo(), step.getProperties(), buildInfo.hashCode());

            String username = step.getCredentialsConfig().provideUsername(build.getParent());
            String password = step.getCredentialsConfig().providePassword(build.getParent());

            JenkinsBuildInfoLog log = new JenkinsBuildInfoLog(listener);
            String deprecationMessage = "It looks like you are using the following deprecated signature of creating Artifactory.docker instance:\n" +
                    "    def rtDocker = Artifactory.docker [credentialsId: 'credentialsId'], [host: 'tcp://<daemon IP>:<daemon port>']\n" +
                    "Please use the following signature of the method, adding the Artifactory server as argument:\n" +
                    "    def rtDocker = Artifactory.docker <server: server>, [host: 'tcp://<daemon IP>:<daemon port>']\n" +
                    "The new method signature does not require setting up the Build-Info Proxy.";

            log.warn(deprecationMessage);

            DockerAgentUtils.pushImage(launcher, log, step.getImage(), username, password, step.getHost());
            if (!DockerAgentUtils.updateImageParentOnAgents(log, step.getImage(), step.getHost(), buildInfo.hashCode())) {
                getContext().onFailure(new IllegalStateException("Build info capturing failed for docker image: " +
                        step.getImage() + " check build info proxy configuration."));
                return null;
            }

            log.info("Successfully pushed docker image: " + step.getImage());
            return buildInfo;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DockerPushWithProxyStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "dockerPushWithProxyStep";
        }

        @Override
        public String getDisplayName() {
            return "Artifactory docker push";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}

