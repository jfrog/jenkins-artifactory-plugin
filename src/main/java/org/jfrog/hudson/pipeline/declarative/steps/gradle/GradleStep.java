package org.jfrog.hudson.pipeline.declarative.steps.gradle;

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
import org.jfrog.hudson.pipeline.executors.GradleExecutor;
import org.jfrog.hudson.pipeline.types.GradleBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class GradleStep extends AbstractStepImpl {

    private GradleBuild gradleBuild;
    private BuildInfo buildInfo;
    private String deployerId;
    private String resolverId;
    private String buildFile;
    private String switches;
    private String rootDir;
    private String tasks;

    @DataBoundConstructor
    public GradleStep() {
        this.gradleBuild = new GradleBuild();
    }

    @DataBoundSetter
    public void setBuildInfo(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    @DataBoundSetter
    public void setDeployerId(String deployerId) {
        this.deployerId = deployerId;
    }

    @DataBoundSetter
    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    @DataBoundSetter
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    @DataBoundSetter
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    @DataBoundSetter
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    @DataBoundSetter
    public void setSwitches(String switches) {
        this.switches = switches;
    }

    @DataBoundSetter
    public void setTool(String tool) {
        gradleBuild.setTool(tool);
    }

    @DataBoundSetter
    public void setUseWrapper(boolean useWrapper) {
        gradleBuild.setUseWrapper(useWrapper);
    }

    @DataBoundSetter
    public void setUsesPlugin(boolean usesPlugin) {
        gradleBuild.setUsesPlugin(usesPlugin);
    }

    private GradleBuild getGradleBuild() {
        return this.gradleBuild;
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
        private transient GradleStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected BuildInfo run() throws Exception {
            GradleBuild gradleBuild = step.getGradleBuild();
            GradleExecutor gradleExecutor = new GradleExecutor(build, gradleBuild, step.getTasks(), step.getBuildFile(), step.getRootDir(), step.getSwitches(), step.getBuildInfo(), env, ws, listener, launcher);
            gradleExecutor.execute();
            return gradleExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GradleStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtGradle";
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
