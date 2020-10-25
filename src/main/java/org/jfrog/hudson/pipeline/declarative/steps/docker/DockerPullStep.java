package org.jfrog.hudson.pipeline.declarative.steps.docker;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.DockerPullExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public class DockerPullStep extends AbstractStepImpl {

    private final String serverId;
    private final String image;
    private final String sourceRepo;
    private String host;
    private String buildNumber;
    private String buildName;
    private String javaArgs;

    @DataBoundConstructor
    public DockerPullStep(String serverId, String image, String sourceRepo) {
        this.serverId = serverId;
        this.image = image;
        this.sourceRepo = sourceRepo;
    }

    @DataBoundSetter
    public void setHost(String host) {
        this.host = host;
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient DockerPullStep step;

        @Inject
        public Execution(DockerPullStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.buildName, step.buildNumber);
            ArtifactoryServer pipelineServer = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            DockerPullExecutor dockerExecutor = new DockerPullExecutor(pipelineServer, buildInfo, build, step.image, step.sourceRepo, step.host, step.javaArgs, launcher, listener, ws, env);
            dockerExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(dockerExecutor.getBuildInfo(), ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DockerPullStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtDockerPull";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory docker pull";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
