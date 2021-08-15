package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.GoPublishExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class GoPublishStep extends AbstractStepImpl {
    static final String STEP_NAME = "artifactoryGoPublish";
    private BuildInfo buildInfo;
    private GoBuild goBuild;
    private String path;
    private String version;
    private String module;
    private String javaArgs; // Added to allow java remote debugging

    @DataBoundConstructor
    public GoPublishStep(BuildInfo buildInfo, GoBuild goBuild, String path, String version, String args, String module, String javaArgs) {
        this.buildInfo = buildInfo;
        this.goBuild = goBuild;
        this.path = path;
        this.version = version;
        this.module = module;
        this.javaArgs = javaArgs;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient GoPublishStep step;

        @Inject
        public Execution(GoPublishStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            GoPublishExecutor goPublishExecutor = new GoPublishExecutor(listener, step.buildInfo, launcher, step.goBuild, step.javaArgs, step.path, step.module, ws, env, build, step.version);
            goPublishExecutor.execute();
            return goPublishExecutor.getBuildInfo();
        }

        @Override
        public ArtifactoryServer getUsageReportServer() {
            Deployer deployer = step.goBuild.getDeployer();
            if (deployer != null) {
                return deployer.getArtifactoryServer();
            }
            return null;
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
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
            return "Run Artifactory Go Publish command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}