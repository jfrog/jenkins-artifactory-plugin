package org.jfrog.hudson.pipeline.declarative.steps.maven;

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
import org.jfrog.hudson.pipeline.types.MavenBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.MavenResolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Objects;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getArtifactoryServer;

@SuppressWarnings("unused")
public class MavenStep extends AbstractStepImpl {

    private MavenBuild mavenBuild;
    private BuildInfo buildInfo;
    private String deployerId;
    private String resolverId;
    private String goal;
    private String pom;

    @DataBoundConstructor
    public MavenStep(String pom, String goals) {
        mavenBuild = new MavenBuild();
        this.goal = Objects.toString(goals, "");
        this.pom = Objects.toString(pom, "");
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
    public void setTool(String tool) {
        mavenBuild.setTool(tool);
    }

    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setOptions(String options) {
        mavenBuild.setOpts(options);
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
            setDeployer(buildNumber);
            setResolver(buildNumber);
        }

        private void setDeployer(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.getDeployerId())) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, MavenDeployerStep.STEP_NAME, step.getDeployerId());
            MavenDeployer mavenDeployer = Utils.mapper().treeToValue(buildDataFile.get(MavenDeployerStep.STEP_NAME), MavenDeployer.class);
            step.getMavenBuild().setDeployer(mavenDeployer);
            mavenDeployer.setServer(getArtifactoryServer(listener, build, ws, getContext(), buildNumber, buildDataFile));
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.getResolverId())) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, MavenResolverStep.STEP_NAME, step.getResolverId());
            MavenResolver mavenResolver = Utils.mapper().treeToValue(buildDataFile.get(MavenResolverStep.STEP_NAME), MavenResolver.class);
            step.getMavenBuild().setResolver(mavenResolver);
            mavenResolver.setServer(getArtifactoryServer(listener, build, ws, getContext(), buildNumber, buildDataFile));
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
