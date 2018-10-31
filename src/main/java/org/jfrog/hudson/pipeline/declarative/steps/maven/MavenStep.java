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
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.pipeline.executors.MavenExecutor;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.MavenBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.MavenResolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("unused")
public class MavenStep extends AbstractStepImpl {

    private MavenBuild mavenBuild;
    private BuildInfo buildInfo;
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
    public void setTool(String tool) {
        mavenBuild.setTool(tool);
    }

    @DataBoundSetter
    public void setOptions(String options) {
        mavenBuild.setOpts(options);
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
            setMavenBuild();
            MavenExecutor mavenExecutor = new MavenExecutor(listener, launcher, build, ws, env, step.mavenBuild, step.pom, step.goals, step.buildInfo);
            mavenExecutor.execute();
            return null;
        }

        private void setMavenBuild() throws IOException, InterruptedException {
            String buildNumber = DeclarativePipelineUtils.getBuildNumberFromStep(getContext());
            setDeployer(buildNumber);
            setResolver(buildNumber);
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.deployerId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, MavenDeployerStep.STEP_NAME, step.deployerId);
            MavenDeployer mavenDeployer = Utils.mapper().treeToValue(buildDataFile.get(MavenDeployerStep.STEP_NAME), MavenDeployer.class);
            step.mavenBuild.setDeployer(mavenDeployer);
            mavenDeployer.setServer(getArtifactoryServer(buildNumber, buildDataFile));
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.resolverId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, MavenResolverStep.STEP_NAME, step.resolverId);
            MavenResolver mavenResolver = Utils.mapper().treeToValue(buildDataFile.get(MavenResolverStep.STEP_NAME), MavenResolver.class);
            step.mavenBuild.setResolver(mavenResolver);
            mavenResolver.setServer(getArtifactoryServer(buildNumber, buildDataFile));
        }

        private ArtifactoryServer getArtifactoryServer(String buildNumber, BuildDataFile buildDataFile) throws IOException, InterruptedException {
            JsonNode serverId = buildDataFile.get("serverId");
            if (serverId.isNull()) {
                return null;
            }
            return DeclarativePipelineUtils.getArtifactoryServer(listener, build, ws, getContext(), buildNumber, serverId.asText());
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
