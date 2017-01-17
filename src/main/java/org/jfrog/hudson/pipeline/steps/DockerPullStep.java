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
import org.jfrog.hudson.pipeline.docker.utils.DockerAgentUtils;
import org.jfrog.hudson.pipeline.docker.utils.DockerUtils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by romang on 5/2/16.
 */
public class DockerPullStep extends AbstractStepImpl {

    private final String image;
    private CredentialsConfig credentialsConfig;
    private String host;
    private final BuildInfo buildInfo;


    @DataBoundConstructor
    public DockerPullStep(String image, CredentialsConfig credentialsConfig, String host, BuildInfo buildInfo) {
        this.image = image;
        this.credentialsConfig = credentialsConfig;
        this.host = host;
        this.buildInfo = buildInfo;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
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

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient DockerPullStep step;

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
            if (step.getImage() == null) {
                getContext().onFailure(new MissingArgumentException("Missing 'image' parameter"));
                return null;
            }

            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            String imageTag = step.getImage();
            if (!DockerUtils.isImageVersioned(imageTag)) {
                imageTag += ":latest";
            }

            String username = step.getCredentialsConfig().provideUsername(build.getParent());
            String password = step.getCredentialsConfig().providePassword(build.getParent());

            DockerAgentUtils.pullImage(launcher, imageTag, username, password, step.getHost());
            JenkinsBuildInfoLog log = new JenkinsBuildInfoLog(listener);
            log.info("Successfully pulled docker image: " + imageTag);
            return buildInfo;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DockerPullStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "dockerPullStep";
        }

        @Override
        public String getDisplayName() {
            return "Artifactory docker pull";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}

