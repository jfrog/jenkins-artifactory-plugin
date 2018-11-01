package org.jfrog.hudson.pipeline.declarative.steps.generic;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.SpecConfiguration;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.SpecUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

@SuppressWarnings("unused")
public class GenericStep extends AbstractStepImpl {
    protected String serverId;
    protected String spec;
    private String customBuildNumber;
    private String customBuildName;
    private String specPath;

    @DataBoundConstructor
    public GenericStep(String serverId) {
        this.serverId = serverId;
    }

    @DataBoundSetter
    public void setSpec(String spec) {
        this.spec = spec;
    }

    @DataBoundSetter
    public void setSpecPath(String specPath) {
        this.specPath = specPath;
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.customBuildName = buildName;
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        this.customBuildNumber = buildNumber;
    }

    public static abstract class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        public static final long serialVersionUID = 1L;

        protected String spec;
        protected BuildInfo buildInfo;
        protected ArtifactoryServer artifactoryServer;

        void setGenericParameters(TaskListener listener, Run build, FilePath ws, EnvVars env, GenericStep step, StepContext context) throws IOException, InterruptedException {
            String buildNumber = DeclarativePipelineUtils.getBuildNumber(getContext());

            // Set spec
            SpecConfiguration specConfiguration = new SpecConfiguration(step.spec, step.specPath);
            spec = SpecUtils.getSpecStringFromSpecConf(specConfiguration, env, ws, listener.getLogger());

            // Set Build Info
            buildInfo = DeclarativePipelineUtils.getBuildInfo(listener, ws, context, step.customBuildName, step.customBuildNumber);

            // Set Artifactory server
            org.jfrog.hudson.pipeline.types.ArtifactoryServer pipelineServer = DeclarativePipelineUtils.getArtifactoryServer(listener, build, ws, getContext(), step.serverId);
            artifactoryServer = Utils.prepareArtifactoryServer(null, pipelineServer);
        }
    }
}
