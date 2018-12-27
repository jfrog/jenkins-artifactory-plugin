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
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GenericUploadExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.kohsuke.stapler.DataBoundConstructor;

public class UploadStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private String spec;
    private ArtifactoryServer server;
    private boolean failNoOp;

    @DataBoundConstructor
    public UploadStep(String spec, BuildInfo buildInfo, boolean failNoOp, ArtifactoryServer server) {
        this.spec = spec;
        this.buildInfo = buildInfo;
        this.failNoOp = failNoOp;
        this.server = server;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public boolean getFailNoOp() {
        return failNoOp;
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
        private transient UploadStep step;

        @Override
        protected BuildInfo run() throws Exception {
            GenericUploadExecutor genericUploadExecutor = new GenericUploadExecutor(Utils.prepareArtifactoryServer(null, step.getServer()), listener, build, ws, step.getBuildInfo(), getContext(), Util.replaceMacro(step.getSpec(), env), step.getFailNoOp());
            genericUploadExecutor.execute();
            BuildInfo buildInfo = genericUploadExecutor.getBuildInfo();
            new BuildInfoAccessor(buildInfo).captureVariables(env, build, listener);
            return buildInfo;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(UploadStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return "artifactoryUpload";
        }

        @Override
        public String getDisplayName() {
            return "Upload artifacts";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
