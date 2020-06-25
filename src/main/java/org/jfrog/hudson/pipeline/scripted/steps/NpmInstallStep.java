package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.NpmInstallExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings("unused")
public class NpmInstallStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String javaArgs;
    private String path;
    private String args;
    private String module;

    @DataBoundConstructor
    public NpmInstallStep(BuildInfo buildInfo, NpmBuild npmBuild, String javaArgs, String path, String args, String module) {
        this.buildInfo = buildInfo;
        this.npmBuild = npmBuild;
        this.javaArgs = javaArgs;
        this.path = path;
        this.args = args;
        this.module = module;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient NpmInstallStep step;

        @Inject
        public Execution(NpmInstallStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo run() throws Exception {
            Utils.addNpmToPath(ws, listener, env, launcher, step.npmBuild.getTool());
            NpmInstallExecutor npmInstallExecutor = new NpmInstallExecutor(step.buildInfo, launcher, step.npmBuild, step.javaArgs, step.args, ws, step.path, step.module, env, listener, build);
            npmInstallExecutor.execute();
            return npmInstallExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmInstallStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNpmInstall";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory npm install";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
