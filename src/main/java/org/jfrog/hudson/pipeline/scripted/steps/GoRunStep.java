package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.GoRunExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class GoRunStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private GoBuild goBuild;
    private String path;
    private String goCmdArgs;
    private String module;

    @DataBoundConstructor
    public GoRunStep(BuildInfo buildInfo, GoBuild goBuild, String path, String goCmdArgs, String args, String module) {
        this.buildInfo = buildInfo;
        this.goBuild = goBuild;
        this.path = path;
        this.goCmdArgs = goCmdArgs;
        this.module = module;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient GoRunStep step;

        @Inject
        public Execution(GoRunStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            GoRunExecutor goRunExecutor = new GoRunExecutor(getContext(), step.buildInfo, step.goBuild, step.path, step.goCmdArgs, step.module, ws, listener, env, build);
            goRunExecutor.execute();
            return goRunExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GoRunStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryGoRun";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory Go command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}