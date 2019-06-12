package org.jfrog.hudson.pipeline.declarative.steps.xray;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.XrayExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.XrayScanConfig;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Map;

/**
 * @author Alexei Vainshtein
 */
@SuppressWarnings("unused")
public class XrayScanStep extends AbstractStepImpl {

    private String serverId;
    private Map<String, Object> params;

    @DataBoundConstructor
    public XrayScanStep(String serverId) {
        this.serverId = serverId;
    }

    @DataBoundSetter
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        @Inject(optional = true)
        private transient XrayScanStep step;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @Override
        protected Void run() throws Exception {
            XrayScanConfig xrayScanConfig = Utils.createXrayScanConfig(step.params);
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            XrayExecutor xrayExecutor = new XrayExecutor(xrayScanConfig, listener, server, build);
            xrayExecutor.execute();
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(XrayScanStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "xrayScan";
        }

        @Override
        public String getDisplayName() {
            return "run Xray scan";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
