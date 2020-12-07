package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.GetArtifactoryServerExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by romang on 4/21/16.
 */
public class GetArtifactoryServerStep extends AbstractStepImpl {
    private String artifactoryServerID;

    @DataBoundConstructor
    public GetArtifactoryServerStep(String artifactoryServerID) {
        this.artifactoryServerID = artifactoryServerID;
    }

    private String getArtifactoryServerID() {
        return artifactoryServerID;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<ArtifactoryServer> {

        private transient GetArtifactoryServerStep step;

        @Inject
        public Execution(GetArtifactoryServerStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected org.jfrog.hudson.pipeline.common.types.ArtifactoryServer runStep() throws Exception {
            String artifactoryServerID = step.getArtifactoryServerID();
            GetArtifactoryServerExecutor getArtifactoryServerExecutor = new GetArtifactoryServerExecutor(build, getContext(), artifactoryServerID);
            getArtifactoryServerExecutor.execute();
            return getArtifactoryServerExecutor.getArtifactoryServer();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "getArtifactoryServer";
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
