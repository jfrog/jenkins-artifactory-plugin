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
import org.jfrog.hudson.pipeline.common.executors.GradleExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GradleBuild;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class ArtifactoryGradleBuild extends AbstractStepImpl {

    private GradleBuild gradleBuild;
    private String tasks;
    private String buildFile;
    private String rootDir;
    private String switches;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public ArtifactoryGradleBuild(GradleBuild gradleBuild, String rootDir, String buildFile, String tasks, String switches, BuildInfo buildInfo) {
        this.gradleBuild = gradleBuild;
        this.tasks = tasks;
        this.rootDir = rootDir;
        this.buildFile = buildFile;
        this.switches = switches;
        this.buildInfo = buildInfo;
    }

    private GradleBuild getGradleBuild() {
        return gradleBuild;
    }

    private String getSwitches() {
        return switches;
    }

    private String getTasks() {
        return tasks;
    }

    private String getBuildFile() {
        return buildFile;
    }

    private BuildInfo getBuildInfo() {
        return buildInfo;
    }

    private String getRootDir() {
        return rootDir;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient ArtifactoryGradleBuild step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected BuildInfo run() throws Exception {
            GradleExecutor gradleExecutor = new GradleExecutor(build, step.getGradleBuild(), step.getTasks(), step.getBuildFile(), step.getRootDir(), step.getSwitches(), step.getBuildInfo(), env, ws, listener, launcher);
            gradleExecutor.execute();
            return gradleExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ArtifactoryGradleBuild.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "ArtifactoryGradleBuild";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory gradle";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
