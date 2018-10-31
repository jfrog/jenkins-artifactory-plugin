package org.jfrog.hudson.pipeline.declarative.steps.generic;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
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
    protected BuildInfo buildInfo;
    protected String spec;
    protected String specPath;
    protected String serverId;

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

    public static abstract class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        protected static final long serialVersionUID = 1L;

        protected String spec;
        protected String buildNumber;
        protected ArtifactoryServer artifactoryServer;

        void setGenericParameters(TaskListener listener, Run build, FilePath ws, EnvVars env, GenericStep step) throws IOException, InterruptedException {
            SpecConfiguration specConfiguration = new SpecConfiguration(step.spec, step.specPath);
            spec = SpecUtils.getSpecStringFromSpecConf(specConfiguration, env, ws, listener.getLogger());
            buildNumber = DeclarativePipelineUtils.getBuildNumberFromStep(getContext());
            org.jfrog.hudson.pipeline.types.ArtifactoryServer pipelineServer = DeclarativePipelineUtils.getArtifactoryServer(listener, build, ws, getContext(), buildNumber, step.serverId);
            artifactoryServer = Utils.prepareArtifactoryServer(null, pipelineServer);
        }
    }
}
