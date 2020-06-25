package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.NpmPublishExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings("unused")
public class NpmPublishStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String javaArgs;
    private String path;
    private String module;

    @DataBoundConstructor
    public NpmPublishStep(BuildInfo buildInfo, NpmBuild npmBuild, String path, String javaArgs, String args, String module) {
        this.buildInfo = buildInfo;
        this.npmBuild = npmBuild;
        this.javaArgs = javaArgs;
        this.path = path;
        this.module = module;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient NpmPublishStep step;

        @Inject
        public Execution(NpmPublishStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo run() throws Exception {
            Utils.addNpmToPath(ws, listener, env, launcher, step.npmBuild.getTool());
            NpmPublishExecutor npmPublishExecutor = new NpmPublishExecutor(listener, step.buildInfo, launcher, step.npmBuild, step.javaArgs, step.path, step.module, ws, env, build);
            npmPublishExecutor.execute();
            return npmPublishExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNpmPublish";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory npm publish";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}