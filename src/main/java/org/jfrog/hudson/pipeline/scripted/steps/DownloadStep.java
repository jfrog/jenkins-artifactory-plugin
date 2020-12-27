package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GenericDownloadExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class DownloadStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private boolean failNoOp;
    private String spec;
    private String module;
    private ArtifactoryServer server;

    @DataBoundConstructor
    public DownloadStep(String spec, BuildInfo buildInfo, boolean failNoOp, String module, ArtifactoryServer server) {
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

        private transient DownloadStep step;

        @Inject
        public Execution(DownloadStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            GenericDownloadExecutor genericDownloadExecutor = new GenericDownloadExecutor(Utils.prepareArtifactoryServer(null, step.getServer()),
                    this.listener, this.build, this.ws, step.getBuildInfo(), Util.replaceMacro(step.getSpec(), env), step.getFailNoOp(), step.getModule());
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
