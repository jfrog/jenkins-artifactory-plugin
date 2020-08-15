package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.cli.MissingArgumentException;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.extractor.docker.DockerUtils;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.DockerPullExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by romang on 5/2/16.
 */
public class DockerPullStep extends AbstractStepImpl {

    private final String image;
    private final ArtifactoryServer server;
    private final BuildInfo buildInfo;
    private String host;
    private String javaArgs;


    @DataBoundConstructor
    public DockerPullStep(String image, String host, String javaArgs, BuildInfo buildInfo, ArtifactoryServer server) {
        this.image = image;
        this.host = host;
        this.buildInfo = buildInfo;
        this.server = server;
        this.javaArgs = javaArgs;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public String getImage() {
        return image;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public String getHost() {
        return host;
    }

    public String getJavaArgs() {
        return javaArgs;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient DockerPullStep step;

        @Inject
        public Execution(DockerPullStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

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

            ArtifactoryServer server = step.getServer();
            DockerPullExecutor dockerExecutor = new DockerPullExecutor(server, buildInfo, build, step.image, step.host, step.javaArgs, launcher, listener, ws, env);
            dockerExecutor.execute();
            JenkinsBuildInfoLog log = new JenkinsBuildInfoLog(listener);
            log.info("Successfully pulled docker image: " + imageTag);
            return dockerExecutor.getBuildInfo();
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

