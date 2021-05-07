package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GetJFrogPlatformInstancesExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by romang on 4/21/16.
 */
public class GetArtifactoryServerStep extends AbstractStepImpl {
    static final String STEP_NAME = "getArtifactoryServer";
    private final String artifactoryServerID;
    private ArtifactoryServer artifactoryServer;

    @DataBoundConstructor
    public GetArtifactoryServerStep(String artifactoryServerID) {
        this.artifactoryServerID = artifactoryServerID;
    }

    private String getArtifactoryServerID() {
        return artifactoryServerID;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<ArtifactoryServer> {

        private transient final GetArtifactoryServerStep step;

        @Inject
        public Execution(GetArtifactoryServerStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected ArtifactoryServer runStep() throws Exception {
            String artifactoryServerID = step.getArtifactoryServerID();
            // JFrogInstancesID is the same as its ArtifactoryServerID
            GetJFrogPlatformInstancesExecutor getJFrogPlatformInstancesExecutor = new GetJFrogPlatformInstancesExecutor(build, artifactoryServerID);
            getJFrogPlatformInstancesExecutor.execute();
            step.artifactoryServer = getJFrogPlatformInstancesExecutor.getJFrogPlatformInstance().getArtifactoryServer();
            return step.artifactoryServer;
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() {
            return Utils.prepareArtifactoryServer(null, step.artifactoryServer);
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Get Artifactory server from Jenkins config";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
