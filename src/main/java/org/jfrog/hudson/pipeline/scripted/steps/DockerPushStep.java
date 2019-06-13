package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.cli.MissingArgumentException;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.DockerExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by romang on 5/2/16.
 */
@SuppressWarnings("unused")
public class DockerPushStep extends AbstractStepImpl {

    private final String image;
    private final ArtifactoryServer server;
    private String host;
    private BuildInfo buildInfo;
    private String targetRepo;
    // Properties to attach to the deployed docker layers.
    private ArrayListMultimap<String, String> properties;

    @DataBoundConstructor
    public DockerPushStep(String image, String host, String targetRepo,
                          BuildInfo buildInfo, ArrayListMultimap<String, String> properties, ArtifactoryServer server) {

        this.image = image;
        this.host = host;
        this.targetRepo = targetRepo;
        this.buildInfo = buildInfo;
        this.properties = properties;
        this.server = server;
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

        @Override
        protected BuildInfo run() throws Exception {
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
            DockerExecutor dockerExecutor = new DockerExecutor(server, buildInfo, build, step.image, step.targetRepo, step.host, launcher, step.properties, listener);
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