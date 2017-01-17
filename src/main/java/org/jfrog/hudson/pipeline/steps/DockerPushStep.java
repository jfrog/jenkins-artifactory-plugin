package org.jfrog.hudson.pipeline.steps;

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
public class DockerPushStep extends AbstractStepImpl {

    private final String image;
    private CredentialsConfig credentialsConfig;
    private String host;
    private BuildInfo buildInfo;
    private String targetRepo;

    @DataBoundConstructor
    public DockerPushStep(String image, CredentialsConfig credentialsConfig, String host, String targetRepo, BuildInfo buildInfo) {
        this.image = image;
        this.credentialsConfig = credentialsConfig;
        this.host = host;
        this.buildInfo = buildInfo;
        this.targetRepo = targetRepo;
    }

    public String getImage() {
        return image;
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
        private transient DockerPushStep step;

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
            DockerAgentUtils.registerImageOnAgents(launcher, step.getImage(), step.getHost(), step.getTargetRepo(), buildInfo.hashCode());

            String username = step.getCredentialsConfig().provideUsername(build.getParent());
            String password = step.getCredentialsConfig().providePassword(build.getParent());

            JenkinsBuildInfoLog log = new JenkinsBuildInfoLog(listener);
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
            super(DockerPushStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "dockerPushStep";
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

