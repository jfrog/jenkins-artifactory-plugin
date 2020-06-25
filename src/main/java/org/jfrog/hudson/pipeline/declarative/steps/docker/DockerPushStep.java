package org.jfrog.hudson.pipeline.declarative.steps.docker;

import com.google.common.collect.ArrayListMultimap;
import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.DockerExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

/**
 * @author Alexei Vainshtein
 */

@SuppressWarnings("unused")
public class DockerPushStep extends AbstractStepImpl {

    private String serverId;
    private String image;
    private String host;
    private String buildNumber;
    private String buildName;
    private String targetRepo;
    private ArrayListMultimap<String, String> properties = ArrayListMultimap.create();

    @DataBoundConstructor
    public DockerPushStep(String serverId, String image, String targetRepo) {
        this.serverId = serverId;
        this.image = image;
        this.targetRepo = targetRepo;
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
    public void setProperties(String properties) {
        String[] props = properties.split(";");
        // Extracts the key,value property
        for (String property : props) {
            String[] keyValue = property.split("=");
            if (keyValue.length == 2) {
                this.properties.put(keyValue[0], keyValue[1]);
            }
        }
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient DockerPushStep step;

        @Inject
        public Execution(DockerPushStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.buildName, step.buildNumber);
            org.jfrog.hudson.pipeline.common.types.ArtifactoryServer pipelineServer = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            DockerExecutor dockerExecutor = new DockerExecutor(pipelineServer, buildInfo, build, step.image, step.targetRepo, step.host, launcher, step.properties, listener, env);
            dockerExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(dockerExecutor.getBuildInfo(), ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DockerPushStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtDockerPush";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory docker push";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
