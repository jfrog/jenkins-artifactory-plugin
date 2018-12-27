package org.jfrog.hudson.pipeline.declarative.steps.generic;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.executors.GenericUploadExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class UploadStep extends GenericStep {

    @DataBoundConstructor
    public UploadStep(String serverId) {
        super(serverId);
    }

    public static class Execution extends GenericStep.Execution {

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected Void run() throws Exception {
            setGenericParameters(listener, build, ws, env, step, getContext());
            GenericUploadExecutor genericUploadExecutor = new GenericUploadExecutor(artifactoryServer, listener, build, ws, buildInfo, getContext(), spec, step.failNoOp);
            genericUploadExecutor.execute();
            BuildInfo buildInfo = genericUploadExecutor.getBuildInfo();
            new BuildInfoAccessor(buildInfo).captureVariables(env, build, listener);
            DeclarativePipelineUtils.saveBuildInfo(buildInfo, ws, build, new JenkinsBuildInfoLog(listener));
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
