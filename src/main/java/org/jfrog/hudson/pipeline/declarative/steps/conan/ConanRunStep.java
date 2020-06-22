package org.jfrog.hudson.pipeline.declarative.steps.conan;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.common.types.ConanClient;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ConanRunStep extends AbstractStepImpl {

    private String clientId;
    private String command;
    private String customBuildNumber;
    private String customBuildName;

    @DataBoundConstructor
    public ConanRunStep(String clientId, String command) {
        this.clientId = clientId;
        this.command = command;
    }

    @DataBoundSetter
    public void setBuildNumber(String customBuildNumber) {
        this.customBuildNumber = customBuildNumber;
    }

    @DataBoundSetter
    public void setBuildName(String customBuildName) {
        this.customBuildName = customBuildName;
    }

    public String getCommand() {
        return this.command;
    }

    public String getClientId() {
        return this.clientId;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient ConanRunStep step;

        @Override
        protected Void run() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            ConanClient conanClient = DeclarativePipelineUtils.buildConanClient(step.getClientId(), buildNumber, ConanClientStep.STEP_NAME, launcher, ws, env);
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.customBuildName, step.customBuildNumber);
            ConanExecutor conanExecutor = new ConanExecutor(buildInfo, conanClient.getUserPath(), ws, launcher, listener, env, build);
            conanExecutor.execCommand(step.getCommand());
            DeclarativePipelineUtils.saveBuildInfo(conanExecutor.getBuildInfo(), ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ConanRunStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtConanRun";
        }

        @Override
        public String getDisplayName() {
            return "Run a Conan command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
