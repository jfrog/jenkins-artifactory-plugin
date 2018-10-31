package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.pipeline.executors.GradleExecutor;
import org.jfrog.hudson.pipeline.types.GradleBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.GradleDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.GradleResolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getArtifactoryServer;

@SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public void setBuildInfo(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setDeployerId(String deployerId) {
        this.deployerId = deployerId;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setBuildFile(String buildFile) {
        this.buildFile = buildFile;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setSwitches(String switches) {
        this.switches = switches;
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setTool(String tool) {
        gradleBuild.setTool(tool);
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setUseWrapper(boolean useWrapper) {
        gradleBuild.setUseWrapper(useWrapper);
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setUsesPlugin(boolean usesPlugin) {
        gradleBuild.setUsesPlugin(usesPlugin);
    }

    private String getDeployerId() {
        return this.deployerId;
    }

    private String getResolverId() {
        return this.resolverId;
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

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
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
        protected Void run() throws Exception {
            setGradleBuild();
            GradleExecutor gradleExecutor = new GradleExecutor(build, step.getGradleBuild(), step.getTasks(), step.getBuildFile(), step.getRootDir(), step.getSwitches(), step.getBuildInfo(), env, ws, listener, launcher);
            gradleExecutor.execute();
            return null;
        }

        private void setGradleBuild() throws IOException, InterruptedException {
            String buildNumber = DeclarativePipelineUtils.getBuildNumberFromStep(getContext());
            setDeployer(buildNumber);
            setResolver(buildNumber);
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.getDeployerId())) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, GradleDeployerStep.STEP_NAME, step.getDeployerId());
            GradleDeployer deployer = Utils.mapper().treeToValue(buildDataFile.get(GradleDeployerStep.STEP_NAME), GradleDeployer.class);
            deployer.setServer(getArtifactoryServer(listener, build, ws, getContext(), buildNumber, buildDataFile));
            step.getGradleBuild().setDeployer(deployer);
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.getResolverId())) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, GradleResolverStep.STEP_NAME, step.getResolverId());
            GradleResolver resolver = Utils.mapper().treeToValue(buildDataFile.get(GradleResolverStep.STEP_NAME), GradleResolver.class);
            resolver.setServer(getArtifactoryServer(listener, build, ws, getContext(), buildNumber, buildDataFile));
            step.getGradleBuild().setResolver(resolver);
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GradleStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtGradleRun";
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
