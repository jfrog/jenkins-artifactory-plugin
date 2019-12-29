package org.jfrog.hudson.pipeline.declarative.steps.maven;

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
import org.jfrog.hudson.pipeline.common.executors.MavenExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.MavenDeployer;
import org.jfrog.hudson.pipeline.common.types.builds.MavenBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.MavenResolver;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.PropertyUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Objects;

/**
 * Run Maven-Artifactory task.
 */
@SuppressWarnings("unused")
public class MavenStep extends AbstractStepImpl {

    private MavenBuild mavenBuild;
    private String customBuildNumber;
    private String customBuildName;
    private String deployerId;
    private String resolverId;
    private String goals;
    private String pom;

    @DataBoundConstructor
    public MavenStep(String pom, String goals) {
        mavenBuild = new MavenBuild();
        this.goals = Objects.toString(goals, "");
        this.pom = Objects.toString(pom, "");
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
    public void setTool(String tool) {
        mavenBuild.setTool(tool);
    }

    @DataBoundSetter
    public void setOpts(String opts) {
        mavenBuild.setOpts(opts);
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Inject(optional = true)
        private transient MavenStep step;

        @Override
        protected Void run() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.customBuildName, step.customBuildNumber);
            setMavenBuild();
            MavenExecutor mavenExecutor = new MavenExecutor(listener, launcher, build, ws, env, step.mavenBuild, step.pom, step.goals, buildInfo);
            mavenExecutor.execute();
            buildInfo = mavenExecutor.getBuildInfo();
            DeclarativePipelineUtils.saveBuildInfo(buildInfo, ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        private void setMavenBuild() throws IOException, InterruptedException {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            setDeployer(buildNumber);
            setResolver(buildNumber);
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.deployerId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, MavenDeployerStep.STEP_NAME, step.deployerId);
            if (buildDataFile == null) {
                throw new IOException("Deployer " + step.deployerId + " doesn't exist!");
            }
            MavenDeployer deployer = Utils.mapper().treeToValue(buildDataFile.get(MavenDeployerStep.STEP_NAME), MavenDeployer.class);
            deployer.setServer(getArtifactoryServer(buildNumber, buildDataFile));
            step.mavenBuild.setDeployer(deployer);
            addProperties(buildDataFile);
        }

        private void addProperties(BuildDataFile buildDataFile) {
            JsonNode propertiesNode = buildDataFile.get("properties");
            if (propertiesNode != null) {
                step.mavenBuild.getDeployer().getProperties().putAll(PropertyUtils.getDeploymentPropertiesMap(propertiesNode.asText(), env));
            }
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.resolverId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, MavenResolverStep.STEP_NAME, step.resolverId);
            if (buildDataFile == null) {
                throw new IOException("Resolver " + step.resolverId + " doesn't exist!");
            }
            MavenResolver resolver = Utils.mapper().treeToValue(buildDataFile.get(MavenResolverStep.STEP_NAME), MavenResolver.class);
            resolver.setServer(getArtifactoryServer(buildNumber, buildDataFile));
            step.mavenBuild.setResolver(resolver);
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
            super(MavenStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtMavenRun";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory maven";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
