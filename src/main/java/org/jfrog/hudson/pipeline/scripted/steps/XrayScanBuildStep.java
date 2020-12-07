package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.XrayExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.XrayScanConfig;
import org.jfrog.hudson.pipeline.common.types.XrayScanResult;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

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

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<XrayScanResult> {

        private transient XrayScanBuildStep step;

        @Inject
        public Execution(XrayScanBuildStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected XrayScanResult runStep() throws Exception {
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
