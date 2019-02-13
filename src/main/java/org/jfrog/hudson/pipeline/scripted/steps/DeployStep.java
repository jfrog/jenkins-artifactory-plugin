package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by yahavi on 11/05/2017.
 */
public class DeployStep extends AbstractStepImpl {

    private Deployer deployer;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public DeployStep(Deployer deployer, BuildInfo buildInfo) {
        this.deployer = deployer;
        this.buildInfo = buildInfo;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient DeployStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient TaskListener listener;

        @Override
        protected Boolean run() throws Exception {
            step.deployer.deployArtifacts(step.buildInfo, listener, ws);
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DeployStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "deployArtifacts";
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