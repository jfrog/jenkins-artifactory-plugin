package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GradleExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.GradleDeployer;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.GradleBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.GradleResolver;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.PropertyUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

/**
 * Run Gradle-Artifactory task.
 */
@SuppressWarnings("unused")
public class GradleStep extends AbstractStepImpl {

    private GradleBuild gradleBuild;
    private String customBuildNumber;
    private String customBuildName;
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
    public void setBuildNumber(String customBuildNumber) {
        this.customBuildNumber = customBuildNumber;
    }

    @DataBoundSetter
    public void setBuildName(String customBuildName) {
        this.customBuildName = customBuildName;
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
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.customBuildName, step.customBuildNumber);
            setGradleBuild();
            GradleExecutor gradleExecutor = new GradleExecutor(build, step.gradleBuild, step.tasks, step.buildFile, step.rootDir, step.switches, buildInfo, env, ws, listener, launcher);
            gradleExecutor.execute();
            buildInfo = gradleExecutor.getBuildInfo();
            DeclarativePipelineUtils.saveBuildInfo(buildInfo, ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        private void setGradleBuild() throws IOException, InterruptedException {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            setDeployer(buildNumber);
            setResolver(buildNumber);
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.deployerId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, GradleDeployerStep.STEP_NAME, step.deployerId);
            if (buildDataFile == null) {
                throw new IOException("Deployer " + step.deployerId + " doesn't exist!");
            }
            GradleDeployer deployer = Utils.mapper().treeToValue(buildDataFile.get(GradleDeployerStep.STEP_NAME), GradleDeployer.class);
            deployer.setServer(getArtifactoryServer(buildNumber, buildDataFile));
            step.gradleBuild.setDeployer(deployer);
            addProperties(buildDataFile);
        }

        private void addProperties(BuildDataFile buildDataFile) {
            JsonNode propertiesNode = buildDataFile.get("properties");
            if (propertiesNode != null) {
                step.gradleBuild.getDeployer().getProperties().putAll(PropertyUtils.getDeploymentPropertiesMap(propertiesNode.asText(), env));
            }
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.resolverId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, GradleResolverStep.STEP_NAME, step.resolverId);
            if (buildDataFile == null) {
                throw new IOException("Resolver " + step.resolverId + " doesn't exist!");
            }
            GradleResolver resolver = Utils.mapper().treeToValue(buildDataFile.get(GradleResolverStep.STEP_NAME), GradleResolver.class);
            resolver.setServer(getArtifactoryServer(buildNumber, buildDataFile));
            step.gradleBuild.setResolver(resolver);
        }

        private ArtifactoryServer getArtifactoryServer(String buildNumber, BuildDataFile buildDataFile) throws IOException, InterruptedException {
            JsonNode serverId = buildDataFile.get("serverId");
            if (serverId.isNull()) {
                throw new IllegalArgumentException("server ID is missing");
            }
            return DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), serverId.asText());
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
