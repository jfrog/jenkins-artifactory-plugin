package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.BuildInfoDeployer;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.HashMap;
import java.util.Map;

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

    public static class Execution extends AbstractSynchronousStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;
        @StepContextParameter
        private transient FilePath ws;

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
            BuildInfoDeployer deployer = buildInfo.createDeployer(build, listener, launcher, Utils.prepareArtifactoryServer(null, step.getServer()));
            deployer.deploy();
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
