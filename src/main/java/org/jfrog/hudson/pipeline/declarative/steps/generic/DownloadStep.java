package org.jfrog.hudson.pipeline.declarative.steps.generic;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.executors.GenericDownloadExecutor;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class DownloadStep extends GenericStep {

    @DataBoundConstructor
    public DownloadStep(String serverId) {
        super(serverId);
    }

    public static class Execution extends GenericStep.Execution {
        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient EnvVars env;

        @Inject(optional = true)
        private transient GenericStep step;

        @Override
        protected Void run() throws Exception {
            setGenericParameters(listener, build, ws, env, step);
            GenericDownloadExecutor genericDownloadExecutor = new GenericDownloadExecutor(artifactoryServer, listener, build, ws, step.buildInfo, spec);
            genericDownloadExecutor.execute();
            BuildInfo buildInfo = genericDownloadExecutor.getBuildInfo();
            new BuildInfoAccessor(buildInfo).captureVariables(env, build, listener);
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
