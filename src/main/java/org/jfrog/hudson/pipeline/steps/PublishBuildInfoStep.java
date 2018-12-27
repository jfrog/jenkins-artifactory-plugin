package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.kohsuke.stapler.DataBoundConstructor;

public class PublishBuildInfoStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private ArtifactoryServer server;

    @DataBoundConstructor
    public PublishBuildInfoStep(BuildInfo buildInfo, ArtifactoryServer server) {
        this.buildInfo = buildInfo;
        this.server = server;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient PublishBuildInfoStep step;

        @Override
        protected Boolean run() throws Exception {
            BuildInfoAccessor buildInfo = new BuildInfoAccessor(step.getBuildInfo());
            org.jfrog.hudson.ArtifactoryServer server = Utils.prepareArtifactoryServer(null, step.getServer());
            ArtifactoryBuildInfoClient client = buildInfo.createArtifactoryClient(server, build, listener);
            try {
                buildInfo.createDeployer(build, listener, server, client).deploy();
            } finally {
                client.close();
            }
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(PublishBuildInfoStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return "publishBuildInfo";
        }

        @Override
        public String getDisplayName() {
            return "Publish build Info to Artifactory";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}