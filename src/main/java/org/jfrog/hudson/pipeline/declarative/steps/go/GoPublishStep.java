package org.jfrog.hudson.pipeline.declarative.steps.go;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.GoPublishExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.CommonDeployer;
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
    static final String STEP_NAME = "rtGoPublish";
    private final GoBuild goBuild;
    private String customBuildNumber;
    private String customBuildName;
    private String project;
    private String deployerId;
    private String javaArgs; // Added to allow java remote debugging
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
    public void setProject(String customProject) {
        this.project = customProject;
    }

    @DataBoundSetter
    public void setDeployerId(String deployerId) {
        this.deployerId = deployerId;
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
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

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final GoPublishStep step;

        @Inject
        public Execution(GoPublishStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.customBuildName, step.customBuildNumber, step.project);
            setDeployer(BuildUniqueIdentifierHelper.getBuildNumber(build));
            GoPublishExecutor goPublishExecutor = new GoPublishExecutor(listener, buildInfo, launcher, step.goBuild, step.javaArgs, step.path, step.module, ws, env, build, step.version);
            goPublishExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(goPublishExecutor.getBuildInfo(), rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() throws IOException, InterruptedException {
            CommonDeployer resolver = getDeployer(BuildUniqueIdentifierHelper.getBuildNumber(build));
            if (resolver != null) {
                return resolver.getArtifactoryServer();
            }
            return  null;
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
        }

        private CommonDeployer getDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.deployerId)) {
                return null;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(rootWs, buildNumber, GoDeployerStep.STEP_NAME, step.deployerId);
            if (buildDataFile == null) {
                throw new IOException("Deployer " + step.deployerId + " doesn't exist!");
            }
            return SerializationUtils.createMapper().treeToValue(buildDataFile.get(GoDeployerStep.STEP_NAME), CommonDeployer.class);
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(rootWs, buildNumber, GoDeployerStep.STEP_NAME, step.deployerId);
            CommonDeployer deployer = getDeployer(buildNumber);
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
            return DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, serverId.asText(), true);
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GoPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
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
