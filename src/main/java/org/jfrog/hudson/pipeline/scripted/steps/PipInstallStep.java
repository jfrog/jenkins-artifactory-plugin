package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.PipInstallExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.PipBuild;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by Bar Belity on 07/07/2020.
 */
public class PipInstallStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private PipBuild pipBuild;
    private String javaArgs;
    private String args;
    private String module;
    private String envActivation;

    @DataBoundConstructor
    public PipInstallStep(BuildInfo buildInfo, PipBuild pipBuild, String javaArgs, String args, String envActivation, String module) {
        this.buildInfo = buildInfo;
        this.pipBuild = pipBuild;
        this.javaArgs = javaArgs;
        this.args = args;
        this.envActivation = envActivation;
        this.module = module;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient PipInstallStep step;

        @Inject
        public Execution(PipInstallStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            PipInstallExecutor pipInstallExecutor = new PipInstallExecutor(step.buildInfo, launcher, step.pipBuild, step.javaArgs, step.args, ws, step.envActivation, step.module, env, listener, build);
            pipInstallExecutor.execute();
            return pipInstallExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(PipInstallStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryPipRun";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory pip install";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}
