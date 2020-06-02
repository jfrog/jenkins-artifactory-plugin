package org.jfrog.hudson.pipeline.declarative.steps.go;

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
import org.jfrog.hudson.pipeline.common.executors.GoPublishExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmGoDeployer;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.PropertyUtils;
import org.jfrog.hudson.util.SerializationUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

/**
 * Run go-publish task.
 */
@SuppressWarnings("unused")
public class GoPublishStep extends AbstractStepImpl {

    private GoBuild goBuild;
    private String customBuildNumber;
    private String customBuildName;
    private String deployerId;
    private String path;
    private String module;
    private String version;

    @DataBoundConstructor
    public GoPublishStep() {
        this.goBuild = new GoBuild();
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
    public void setPath(String path) {
        this.path = path;
    }

    @DataBoundSetter
    public void setModule(String module) {
        this.module = module;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient GoPublishStep step;

        @Override
        protected Void run() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.customBuildName, step.customBuildNumber);
            setDeployer(BuildUniqueIdentifierHelper.getBuildNumber(build));
            GoPublishExecutor goPublishExecutor = new GoPublishExecutor(getContext(), buildInfo, step.goBuild, step.path, step.version, step.module, ws, listener, build);
            goPublishExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(goPublishExecutor.getBuildInfo(), ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.deployerId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, GoDeployerStep.STEP_NAME, step.deployerId);
            if (buildDataFile == null) {
                throw new IOException("Deployer " + step.deployerId + " doesn't exist!");
            }
            NpmGoDeployer deployer = SerializationUtils.createMapper().treeToValue(buildDataFile.get(GoDeployerStep.STEP_NAME), NpmGoDeployer.class);
            deployer.setServer(getArtifactoryServer(buildNumber, buildDataFile));
            step.goBuild.setDeployer(deployer);
            addProperties(buildDataFile);
        }

        private void addProperties(BuildDataFile buildDataFile) {
            JsonNode propertiesNode = buildDataFile.get("properties");
            if (propertiesNode != null) {
                step.goBuild.getDeployer().getProperties().putAll(PropertyUtils.getDeploymentPropertiesMap(propertiesNode.asText(), env));
            }
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
            super(GoPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtGoPublish";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory Go publish";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
