package org.jfrog.hudson.pipeline.scripted.steps;

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
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class CreateDockerBuildStep extends AbstractStepImpl {
    static final String STEP_NAME = "createDockerBuildStep";
    private final ArtifactoryServer server;
    private final String kanikoImageFile;
    private final String jibImageFiles;
    private final BuildInfo buildInfo;
    private final String sourceRepo;
    private final String javaArgs;

    @DataBoundConstructor
    public CreateDockerBuildStep(String kanikoImageFile, String jibImageFiles, String sourceRepo, BuildInfo buildInfo, ArtifactoryServer server, String javaArgs) {
        this.kanikoImageFile = kanikoImageFile;
        this.jibImageFiles = jibImageFiles;
        this.sourceRepo = sourceRepo;
        this.buildInfo = buildInfo;
        this.javaArgs = javaArgs;
        this.server = server;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public String getSourceRepo() {
        return sourceRepo;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {
    protected static final long serialVersionUID = 1L;
        private final transient CreateDockerBuildStep step;

        @Inject
        public Execution(CreateDockerBuildStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            BuildInfoProcessRunner dockerExecutor = new CreateDockerBuildExecutor(step.server, buildInfo, build, step.kanikoImageFile, step.jibImageFiles, step.sourceRepo, step.javaArgs, launcher, listener, ws, env);
            dockerExecutor.execute();
            return dockerExecutor.getBuildInfo();
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() {
            return Utils.prepareArtifactoryServer(null, step.getServer());
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateDockerBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Artifactory create Docker build";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}