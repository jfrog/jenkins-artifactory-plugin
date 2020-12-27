package org.jfrog.hudson.pipeline.declarative.steps.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.NpmPublishExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
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
 * Run npm-publish task.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class NpmPublishStep extends AbstractStepImpl {

    private final NpmBuild npmBuild;
    private String customBuildNumber;
    private String customBuildName;
    private String deployerId;
    private String javaArgs; // Added to allow java remote debugging
    private String path;
    private String module;

    @DataBoundConstructor
    public NpmPublishStep() {
        this.npmBuild = new NpmBuild();
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
    public void setTool(String tool) {
        npmBuild.setTool(tool);
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final NpmPublishStep step;

        @Inject
        public Execution(NpmPublishStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.customBuildName, step.customBuildNumber);
            setDeployer(BuildUniqueIdentifierHelper.getBuildNumber(build));
            Utils.addNpmToPath(ws, listener, env, launcher, step.npmBuild.getTool());
            NpmPublishExecutor npmPublishExecutor = new NpmPublishExecutor(listener, buildInfo, launcher, step.npmBuild, step.javaArgs, step.path, step.module, ws, env, build);
            npmPublishExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(npmPublishExecutor.getBuildInfo(), rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.deployerId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(rootWs, buildNumber, NpmDeployerStep.STEP_NAME, step.deployerId);
            if (buildDataFile == null) {
                throw new IOException("Deployer " + step.deployerId + " doesn't exist!");
            }
            CommonDeployer deployer = SerializationUtils.createMapper().treeToValue(buildDataFile.get(NpmDeployerStep.STEP_NAME), CommonDeployer.class);
            deployer.setServer(getArtifactoryServer(buildNumber, buildDataFile));
            step.npmBuild.setDeployer(deployer);
            addProperties(buildDataFile);
        }

        private void addProperties(BuildDataFile buildDataFile) {
            JsonNode propertiesNode = buildDataFile.get("properties");
            if (propertiesNode != null) {
                step.npmBuild.getDeployer().getProperties().putAll(PropertyUtils.getDeploymentPropertiesMap(propertiesNode.asText(), env));
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
            super(NpmPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtNpmPublish";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory npm publish";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
