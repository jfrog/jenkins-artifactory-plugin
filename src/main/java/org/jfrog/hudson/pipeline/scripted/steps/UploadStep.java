package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GenericUploadExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class UploadStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private String spec;
    private ArtifactoryServer server;
    private String module;
    private boolean failNoOp;

    @DataBoundConstructor
    public UploadStep(String spec, BuildInfo buildInfo, boolean failNoOp, String module, ArtifactoryServer server) {
        this.spec = spec;
        this.buildInfo = buildInfo;
        this.failNoOp = failNoOp;
        this.module = module;
        this.server = server;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public boolean getFailNoOp() {
        return failNoOp;
    }

    public String getModule() {
        return module;
    }

    public String getSpec() {
        return spec;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient UploadStep step;

        @Inject
        public Execution(UploadStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            GenericUploadExecutor genericUploadExecutor = new GenericUploadExecutor(Utils.prepareArtifactoryServer(null, step.getServer()), listener, build, ws, step.getBuildInfo(), getContext(), Util.replaceMacro(step.getSpec(), env), step.getFailNoOp(), step.module);
            genericUploadExecutor.execute();
            BuildInfo buildInfo = genericUploadExecutor.getBuildInfo();
            buildInfo.captureVariables(env, build, listener);
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
