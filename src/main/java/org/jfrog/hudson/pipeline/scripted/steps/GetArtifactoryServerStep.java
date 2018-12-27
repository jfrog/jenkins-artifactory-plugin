package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.executors.GetArtifactoryServerExecutor;
import org.kohsuke.stapler.DataBoundConstructor;

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

    public static class Execution extends AbstractSynchronousStepExecution<org.jfrog.hudson.pipeline.common.types.ArtifactoryServer> {

        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient GetArtifactoryServerStep step;

        @Override
        protected org.jfrog.hudson.pipeline.common.types.ArtifactoryServer run() throws Exception {
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
