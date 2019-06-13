package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.executors.XrayExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.XrayScanConfig;
import org.jfrog.hudson.pipeline.common.types.XrayScanResult;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class XrayScanBuildStep extends AbstractStepImpl {

    private ArtifactoryServer server;
    private XrayScanConfig xrayScanConfig;

    @DataBoundConstructor
    public XrayScanBuildStep(XrayScanConfig xrayScanConfig, ArtifactoryServer server) {
        this.xrayScanConfig = xrayScanConfig;
        this.server = server;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public XrayScanConfig getXrayScanConfig() {
        return xrayScanConfig;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<XrayScanResult> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient XrayScanBuildStep step;

        @Override
        protected XrayScanResult run() throws Exception {
            XrayScanConfig xrayScanConfig = step.getXrayScanConfig();
            ArtifactoryServer server = step.getServer();
            XrayExecutor xrayExecutor = new XrayExecutor(xrayScanConfig, listener, server, build);
            xrayExecutor.execute();
            return xrayExecutor.getXrayScanResult();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(XrayScanBuildStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return "xrayScanBuild";
        }

        @Override
        public String getDisplayName() {
            return "Xray build scanning";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
