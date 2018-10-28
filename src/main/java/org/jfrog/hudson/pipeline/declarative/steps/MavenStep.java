package org.jfrog.hudson.pipeline.declarative.steps;

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
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.pipeline.executors.GetArtifactoryServerExecutor;
import org.jfrog.hudson.pipeline.executors.MavenExecutor;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.MavenBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.MavenResolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public class MavenStep extends AbstractStepImpl {

    private MavenBuild mavenBuild;
    private String goal;
    private String pom;
    private String deployerId;
    private String resolverId;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public MavenStep(String pom, String goals) {
        mavenBuild = new MavenBuild();
        this.goal = goals == null ? "" : goals;
        this.pom = pom == null ? "" : pom;
    }

    @DataBoundSetter
    public void setOptions(String options) {
        mavenBuild.setOpts(options);
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


    private String getGoal() {
        return goal;
    }

    private String getPom() {
        return pom;
    }

    private BuildInfo getBuildInfo() {
        return buildInfo;
    }

    private MavenBuild getMavenBuild() {
        return mavenBuild;
    }

    private String getDeployerId() {
        return deployerId;
    }

    private String getResolverId() {
        return resolverId;
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
            MavenExecutor mavenExecutor = new MavenExecutor(listener, launcher, build, ws, env, step.getMavenBuild(), step.getPom(), step.getGoal(), step.getBuildInfo());
            mavenExecutor.execute();
            return null;
        }

        private void setMavenBuild() throws IOException, InterruptedException {
            String buildNumber = DeclarativePipelineUtils.getBuildNumberFromStep(getContext());
            setMavenDeployer(buildNumber);
            setMavenResolver(buildNumber);
        }

        private void setMavenDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isNotBlank(step.getDeployerId())) {
                JsonNode jsonNode = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, MavenDeployStep.STEP_NAME, step.getDeployerId());
                MavenDeployer mavenDeployer = step.getMavenBuild().getDeployer();
                mavenDeployer.setSnapshotRepo(jsonNode.get("snapshotRepo").asText()).setReleaseRepo(jsonNode.get("releaseRepo").asText());
                mavenDeployer.setServer(getArtifactoryServer(buildNumber, jsonNode));
            }
        }

        private void setMavenResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isNotBlank(step.getResolverId())) {
                JsonNode jsonNode = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, MavenResolveStep.STEP_NAME, step.getResolverId());
                MavenResolver mavenResolver = step.getMavenBuild().getResolver();
                mavenResolver.setSnapshotRepo(jsonNode.get("snapshotRepo").asText()).setReleaseRepo(jsonNode.get("releaseRepo").asText());
                mavenResolver.setServer(getArtifactoryServer(buildNumber, jsonNode));
            }
        }

        private ArtifactoryServer getArtifactoryServer(String buildNumber, JsonNode jsonNode) throws IOException, InterruptedException {
            JsonNode serverId = jsonNode.get("serverId");
            if (serverId.isNull()) {
                return null;
            }
            jsonNode = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, CreateServerStep.STEP_NAME, serverId.asText());
            if (jsonNode.isNull()) {
                GetArtifactoryServerExecutor getArtifactoryServerExecutor = new GetArtifactoryServerExecutor(build, getContext(), serverId.asText());
                getArtifactoryServerExecutor.execute();
                return getArtifactoryServerExecutor.getArtifactoryServer();
            }
            String url = jsonNode.get("url").asText();
            JsonNode credentialsIdJson = jsonNode.get("credentialsId");
            if (credentialsIdJson == null || credentialsIdJson.isNull()) {
                String username = jsonNode.get("username").asText();
                String password = jsonNode.get("password").asText();
                return new ArtifactoryServer(url, username, password);
            }
            String credentialsId = credentialsIdJson.asText();
            return new ArtifactoryServer(url, credentialsId);
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(MavenStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtMaven";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory maven";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
