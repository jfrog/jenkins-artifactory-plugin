package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.PublishBuildInfoExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

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

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {

        private transient PublishBuildInfoStep step;

        @Inject
        public Execution(PublishBuildInfoStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean runStep() throws Exception {
            new PublishBuildInfoExecutor(build, listener, step.getBuildInfo(), step.getServer(), ws).execute();
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