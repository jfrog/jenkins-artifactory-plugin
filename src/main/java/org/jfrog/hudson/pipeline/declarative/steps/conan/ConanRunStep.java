package org.jfrog.hudson.pipeline.declarative.steps.conan;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.common.types.ConanClient;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public class ConanRunStep extends AbstractStepImpl {
    static final String STEP_NAME = "rtConanRun";
    private final String clientId;
    private final String command;
    private String customBuildNumber;
    private String customBuildName;
    private String project;

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

    @DataBoundSetter
    public void setProject(String customProject) {
        this.project = customProject;
    }

    public String getCommand() {
        return this.command;
    }

    public String getClientId() {
        return this.clientId;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {
        protected static final long serialVersionUID = 1L;
        private transient final ConanRunStep step;

        @Inject
        public Execution(ConanRunStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            ConanClient conanClient = DeclarativePipelineUtils.buildConanClient(step.getClientId(), buildNumber, ConanClientStep.STEP_NAME, launcher, ws, rootWs, env);
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.customBuildName, step.customBuildNumber, step.project);
            ConanExecutor conanExecutor = new ConanExecutor(buildInfo, conanClient.getUserPath(), ws, launcher, listener, env, build);
            conanExecutor.execCommand(step.getCommand());
            DeclarativePipelineUtils.saveBuildInfo(conanExecutor.getBuildInfo(), rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        @Override
        public ArtifactoryServer getUsageReportServer() throws IOException, InterruptedException {
            return null;
        }

        @Override
        public String getUsageReportFeatureName() {
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
            return STEP_NAME;
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
