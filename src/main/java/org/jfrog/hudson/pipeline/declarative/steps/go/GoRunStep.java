package org.jfrog.hudson.pipeline.declarative.steps.go;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.GoRunExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
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
 * Run go-run task.
 */
@SuppressWarnings("unused")
public class GoRunStep extends AbstractStepImpl {

    private final GoBuild goBuild;
    private String customBuildNumber;
    private String customBuildName;
    private String resolverId;
    private String path;
    private String args;
    private String module;

    @DataBoundConstructor
    public GoRunStep() {
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
    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    @DataBoundSetter
    public void setPath(String path) {
        this.path = path;
    }

    @DataBoundSetter
    public void setArgs(String args) {
        this.args = args;
    }

    @DataBoundSetter
    public void setModule(String module) {
        this.module = module;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final GoRunStep step;

        @Inject
        public Execution(GoRunStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.customBuildName, step.customBuildNumber);
            setResolver(BuildUniqueIdentifierHelper.getBuildNumber(build));
            GoRunExecutor goRunExecutor = new GoRunExecutor(getContext(), buildInfo, step.goBuild, step.path, step.args, step.module, ws, listener, env, build);
            goRunExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(goRunExecutor.getBuildInfo(), rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.resolverId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(rootWs, buildNumber, GoResolverStep.STEP_NAME, step.resolverId);
            if (buildDataFile == null) {
                throw new IOException("Resolver " + step.resolverId + " doesn't exist!");
            }
            CommonResolver resolver = SerializationUtils.createMapper().treeToValue(buildDataFile.get(GoResolverStep.STEP_NAME), CommonResolver.class);
            resolver.setServer(getArtifactoryServer(buildDataFile));
            step.goBuild.setResolver(resolver);
            addProperties(buildDataFile);
        }

        private void addProperties(BuildDataFile buildDataFile) {
            JsonNode propertiesNode = buildDataFile.get("properties");
            if (propertiesNode != null) {
                step.goBuild.getDeployer().getProperties().putAll(PropertyUtils.getDeploymentPropertiesMap(propertiesNode.asText(), env));
            }
        }

        private ArtifactoryServer getArtifactoryServer(BuildDataFile buildDataFile) throws IOException, InterruptedException {
            JsonNode serverId = buildDataFile.get("serverId");
            if (serverId.isNull()) {
                throw new IllegalArgumentException("server ID is missing");
            }
            return DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, getContext(), serverId.asText());
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(GoRunStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtGoRun";
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
