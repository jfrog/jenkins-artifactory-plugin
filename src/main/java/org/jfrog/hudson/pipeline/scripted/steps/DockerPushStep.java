package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.cli.MissingArgumentException;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.DockerPushExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class DockerPushStep extends AbstractStepImpl {

    private final String image;
    private final ArtifactoryServer server;
    private String host;
    private BuildInfo buildInfo;
    private String targetRepo;
    private String javaArgs;
    // Properties to attach to the deployed docker layers.
    private ArrayListMultimap<String, String> properties;

    @DataBoundConstructor
    public DockerPushStep(String image, String host, String targetRepo,
                          BuildInfo buildInfo, ArrayListMultimap<String, String> properties, ArtifactoryServer server, String javaArgs) {

        this.image = image;
        this.host = host;
        this.targetRepo = targetRepo;
        this.buildInfo = buildInfo;
        this.properties = properties;
        this.server = server;
        this.javaArgs = javaArgs;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public String getImage() {
        return image;
    }

    public ArrayListMultimap<String, String> getProperties() {
        return properties;
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

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient DockerPushStep step;

        @Inject
        public Execution(DockerPushStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            if (step.getImage() == null) {
                getContext().onFailure(new MissingArgumentException("Missing 'image' parameter"));
                return null;
            }

            if (step.getTargetRepo() == null) {
                getContext().onFailure(new MissingArgumentException("Missing 'targetRepo' parameter"));
                return null;
            }
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());

            ArtifactoryServer server = step.getServer();
            DockerPushExecutor dockerExecutor = new DockerPushExecutor(server, buildInfo, build, step.image, step.targetRepo, step.host, step.javaArgs, launcher, step.properties, listener, ws, env);
            dockerExecutor.execute();
            return dockerExecutor.getBuildInfo();
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