package org.jfrog.hudson.pipeline.declarative.steps.generic;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.common.executors.GenericUploadExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class UploadStep extends GenericStep {

    @DataBoundConstructor
    public UploadStep(String serverId) {
        super(serverId);
    }

    public static class Execution extends GenericStep.Execution {

        @Inject
        public Execution(GenericStep step, StepContext context) throws IOException, InterruptedException {
            super(step, context);
        }

        @Override
        protected Void run() throws Exception {
            setGenericParameters(step, getContext());
            GenericUploadExecutor genericUploadExecutor = new GenericUploadExecutor(artifactoryServer, listener, build, ws, buildInfo, getContext(), spec, step.failNoOp, step.module);
            genericUploadExecutor.execute();
            BuildInfo buildInfo = genericUploadExecutor.getBuildInfo();
            new BuildInfoAccessor(buildInfo).captureVariables(env, build, listener);
            DeclarativePipelineUtils.saveBuildInfo(buildInfo, rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(UploadStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtUpload";
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
