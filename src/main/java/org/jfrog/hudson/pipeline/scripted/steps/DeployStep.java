package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by yahavi on 11/05/2017.
 */
public class DeployStep extends AbstractStepImpl {
    static final String STEP_NAME = "deployArtifacts";
    private Deployer deployer;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public DeployStep(Deployer deployer, BuildInfo buildInfo) {
        this.deployer = deployer;
        this.buildInfo = buildInfo;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {
    protected static final long serialVersionUID = 1L;
        private transient DeployStep step;

        @Inject
        public Execution(DeployStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean runStep() throws Exception {
            step.deployer.deployArtifacts(step.buildInfo, listener, ws, build);
            return true;
        }

        @Override
        public ArtifactoryServer getUsageReportServer() {
            return step.deployer.getArtifactoryServer();
        }

        @Override
        /**
         * Returns the name of the step and the tool that is used for the deployment e.g. maven or gradle.
         */
        public String getUsageReportFeatureName() {
            return STEP_NAME + "_" + step.deployer.getClass().getSimpleName();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DeployStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Deploy artifacts";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}