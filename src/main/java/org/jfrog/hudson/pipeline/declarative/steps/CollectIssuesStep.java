package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.SpecConfiguration;
import org.jfrog.hudson.pipeline.common.executors.CollectIssuesExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.SpecUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@SuppressWarnings("unused")
public class CollectIssuesStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtCollectIssues";
    private String config;
    private String configPath;
    private String customBuildNumber;
    private String customBuildName;
    private String serverId;

    @DataBoundConstructor
    public CollectIssuesStep(String serverId) {
        this.serverId = serverId;
    }

    @DataBoundSetter
    public void setConfig(String config) {
        this.config = config;
    }

    @DataBoundSetter
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.customBuildName = buildName;
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        this.customBuildNumber = buildNumber;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        protected String config;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient EnvVars env;

        @Inject(optional = true)
        private transient CollectIssuesStep step;

        @Override
        protected Void run() throws Exception {
            // Set spec
            SpecConfiguration specConfiguration = new SpecConfiguration(step.config, step.configPath);
            config = SpecUtils.getSpecStringFromSpecConf(specConfiguration, env, ws, listener.getLogger());

            // Get build info
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.customBuildName, step.customBuildNumber);

            // Get server
            org.jfrog.hudson.pipeline.common.types.ArtifactoryServer pipelineServer = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);

            // Collect issues
            CollectIssuesExecutor collectIssuesExecutor = new CollectIssuesExecutor(build, listener, ws, buildInfo.getName(), config, buildInfo.getIssues(), pipelineServer);
            collectIssuesExecutor.execute();

            DeclarativePipelineUtils.saveBuildInfo(buildInfo, ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CollectIssuesStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Collect issues";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
