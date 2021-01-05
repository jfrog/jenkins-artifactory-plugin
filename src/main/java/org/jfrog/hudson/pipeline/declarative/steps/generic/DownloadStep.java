package org.jfrog.hudson.pipeline.declarative.steps.generic;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.common.executors.GenericDownloadExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class DownloadStep extends GenericStep {

    @DataBoundConstructor
    public DownloadStep(String serverId) {
        super(serverId);
    }

    public static class Execution extends GenericStep.Execution {

        @Inject
        public Execution(GenericStep step, StepContext context) throws IOException, InterruptedException {
            super(step, context);
        }

        @Override
        protected Void runStep() throws Exception {
            setGenericParameters(step, getContext());
            GenericDownloadExecutor genericDownloadExecutor = new GenericDownloadExecutor(artifactoryServer, listener, build, ws, buildInfo, spec, step.failNoOp, step.module);
            genericDownloadExecutor.execute();
            BuildInfo buildInfo = genericDownloadExecutor.getBuildInfo();
            buildInfo.captureVariables(env, build, listener);
            DeclarativePipelineUtils.saveBuildInfo(buildInfo, rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DownloadStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtDownload";
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
