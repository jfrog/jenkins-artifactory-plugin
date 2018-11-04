package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.GenericDownloadExecutor;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.kohsuke.stapler.DataBoundConstructor;

public class DownloadStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private String spec;
    private ArtifactoryServer server;

    @DataBoundConstructor
    public DownloadStep(String spec, BuildInfo buildInfo, ArtifactoryServer server) {
        this.spec = spec;
        this.buildInfo = buildInfo;
        this.server = server;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public String getSpec() {
        return spec;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;
        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient EnvVars env;

        @Inject(optional = true)
        private transient DownloadStep step;

        @Override
        protected BuildInfo run() throws Exception {
            GenericDownloadExecutor genericDownloadExecutor = new GenericDownloadExecutor(Utils.prepareArtifactoryServer(null, step.getServer()),
                    this.listener, this.build, this.ws, step.getBuildInfo(), Util.replaceMacro(step.getSpec(), env));
            genericDownloadExecutor.execute();
            BuildInfo buildInfo = genericDownloadExecutor.getBuildInfo();
            new BuildInfoAccessor(buildInfo).captureVariables(env, build, listener);
            return buildInfo;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DownloadStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return "artifactoryDownload";
        }

        @Override
        public String getDisplayName() {
            return "Download artifacts";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}
