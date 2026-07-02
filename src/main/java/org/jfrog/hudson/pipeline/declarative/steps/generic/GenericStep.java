package org.jfrog.hudson.pipeline.declarative.steps.generic;

import com.google.inject.Inject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.SpecConfiguration;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.SpecUtils;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

/**
 * Use file spec to upload and download artifacts.
 * Allows to use input spec from string parameter or from file.
 */
@SuppressWarnings("unused")
public class GenericStep extends AbstractStepImpl {

    protected String serverId;
    protected String spec;
    private String customBuildNumber;
    private String customBuildName;
    private String project;
    private String specPath;
    boolean failNoOp;
    protected String module;

    GenericStep(String serverId) {
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

    @DataBoundSetter
    public void setProject(String project) {
        this.project = project;
    }

    @DataBoundSetter
    public void setFailNoOp(boolean failNoOp) {
        this.failNoOp = failNoOp;
    }

    @DataBoundSetter
    public void setModule(String module) {
        this.module = module;
    }

    public static abstract class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {
        protected static final long serialVersionUID = 1L;
        protected transient GenericStep step;
        protected ArtifactoryServer artifactoryServer;
        protected BuildInfo buildInfo;
        protected String spec;

        @Inject
        public Execution(GenericStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() throws IOException, InterruptedException {
            return getArtifactoryServer();
        }

        public org.jfrog.hudson.ArtifactoryServer getArtifactoryServer() throws IOException, InterruptedException {
            org.jfrog.hudson.pipeline.common.types.ArtifactoryServer pipelineServer = DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, step.serverId, true);
            return Utils.prepareArtifactoryServer(null, pipelineServer);
        }

        void setGenericParameters(GenericStep step, StepContext context) throws IOException, InterruptedException {
            // Set spec
            SpecConfiguration specConfiguration = new SpecConfiguration(step.spec, step.specPath);
            spec = SpecUtils.getSpecStringFromSpecConf(specConfiguration, env, ws, listener.getLogger());

            // Set Build Info
            buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.customBuildName, step.customBuildNumber, step.project);

            // Set Artifactory server
            artifactoryServer = getArtifactoryServer();
        }
    }
}
