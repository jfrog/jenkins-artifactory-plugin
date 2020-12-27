package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.GoPublishExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class GoPublishStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private GoBuild goBuild;
    private String path;
    private String version;
    private String module;

    @DataBoundConstructor
    public GoPublishStep(BuildInfo buildInfo, GoBuild goBuild, String path, String version, String args, String module) {
        this.buildInfo = buildInfo;
        this.goBuild = goBuild;
        this.path = path;
        this.version = version;
        this.module = module;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient GoPublishStep step;

        @Inject
        public Execution(GoPublishStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            GoPublishExecutor goPublishExecutor = new GoPublishExecutor(getContext(), step.buildInfo, step.goBuild, step.path, step.version, step.module, ws, listener, build);
            goPublishExecutor.execute();
            return goPublishExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GoPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryGoPublish";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory Go Publish command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}