package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.NpmInstallExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.kohsuke.stapler.DataBoundConstructor;

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

    @DataBoundConstructor
    public NpmInstallStep(BuildInfo buildInfo, NpmBuild npmBuild, String javaArgs, String path, String args) {
        this.buildInfo = buildInfo;
        this.npmBuild = npmBuild;
        this.javaArgs = javaArgs;
        this.path = path;
        this.args = args;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient NpmInstallStep step;

        @Override
        protected BuildInfo run() throws Exception {
            String npmExe = Utils.getNpmExe(ws, listener, env, launcher, step.npmBuild.getTool());
            NpmInstallExecutor npmInstallExecutor = new NpmInstallExecutor(step.buildInfo, launcher, step.npmBuild, step.javaArgs, npmExe, step.args, ws, step.path, env, listener, build);
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
