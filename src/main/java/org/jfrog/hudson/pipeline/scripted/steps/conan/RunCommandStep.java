package org.jfrog.hudson.pipeline.scripted.steps.conan;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class RunCommandStep extends AbstractStepImpl {
    private String command;
    private String conanHome;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public RunCommandStep(String command, String conanHome, BuildInfo buildInfo) {
        this.command = command;
        this.conanHome = conanHome;
        this.buildInfo = buildInfo;
    }

    public String getCommand() {
        return this.command;
    }

    public String getConanHome() {
        return this.conanHome;
    }

    public BuildInfo getBuildInfo() {
        return this.buildInfo;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient RunCommandStep step;

        @Inject
        public Execution(RunCommandStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            ConanExecutor conanExecutor = new ConanExecutor(step.getBuildInfo(), step.getConanHome(), ws, launcher, listener, env, build);
            conanExecutor.execCommand(step.getCommand());
            return conanExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(RunCommandStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "runConanCommand";
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