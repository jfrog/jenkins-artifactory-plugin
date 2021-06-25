package org.jfrog.hudson.pipeline.declarative.steps.docker;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.CreateDockerBuildExecutor;
import org.jfrog.hudson.pipeline.common.executors.BuildInfoProcessRunner;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author yahavi
 **/
public class CreateDockerBuild extends AbstractStepImpl {
    static final String STEP_NAME = "rtCreateDockerBuild";
    private final String sourceRepo;
    private final String serverId;

    private String kanikoImageFile;
    private String jibImageFiles;
    private String buildNumber;
    private String buildName;
    private String javaArgs;
    private String project;

    @DataBoundConstructor
    public CreateDockerBuild(String serverId, String sourceRepo) {
        this.sourceRepo = sourceRepo;
        this.serverId = serverId;
    }

    @DataBoundSetter
    public void setKanikoImageFile(String kanikoImageFile) {
        this.kanikoImageFile = kanikoImageFile;
    }

    @DataBoundSetter
    public void setJibImageFiles(String jibImageFiles) {
        this.jibImageFiles = jibImageFiles;
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final CreateDockerBuild step;

        @Inject
        public Execution(CreateDockerBuild step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.buildName, step.buildNumber, step.project);
            ArtifactoryServer pipelineServer = DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, step.serverId, true);
            BuildInfoProcessRunner dockerExecutor = new CreateDockerBuildExecutor(pipelineServer, buildInfo, build, step.kanikoImageFile, step.jibImageFiles, step.sourceRepo, step.javaArgs, launcher, listener, ws, env);
            dockerExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(dockerExecutor.getBuildInfo(), rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() {
            return Utils.prepareArtifactoryServer(step.serverId, null);
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateDockerBuild.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "run Artifactory create Docker build";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
