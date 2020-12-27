package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.NugetRunExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NugetBuild;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class NugetRunStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private NugetBuild nugetBuild;
    private String javaArgs;
    private String args;
    private String module;

    @DataBoundConstructor
    public NugetRunStep(BuildInfo buildInfo, NugetBuild nugetBuild, String javaArgs, String args, String module) {
        this.buildInfo = buildInfo;
        this.nugetBuild = nugetBuild;
        this.javaArgs = javaArgs;
        this.args = args;
        this.module = module;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient NugetRunStep step;

        @Inject
        public Execution(NugetRunStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            NugetRunExecutor nugetRunExecutor = new NugetRunExecutor(step.buildInfo, launcher, step.nugetBuild, step.javaArgs, step.args, ws, step.module, env, listener, build);
            nugetRunExecutor.execute();
            return nugetRunExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NugetRunStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNugetRun";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory NuGet";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
