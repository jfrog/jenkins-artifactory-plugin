package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GetJfrogServersExecutor;
import org.jfrog.hudson.pipeline.common.types.JfrogServers;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class GetJfrogServersStep extends AbstractStepImpl {
    static final String STEP_NAME = "getJfrogServers";
    private final String jfrogServersID;
    private JfrogServers jfrogServers;

    @DataBoundConstructor
    public GetJfrogServersStep(String jfrogServersID) {
        this.jfrogServersID = jfrogServersID;
    }

    private String getJfrogServersID() {
        return jfrogServersID;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<JfrogServers> {

        private transient final GetJfrogServersStep step;

        @Inject
        public Execution(GetJfrogServersStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected JfrogServers runStep() throws Exception {
            String jfrogServersID = step.getJfrogServersID();
            GetJfrogServersExecutor getArtifactoryServerExecutor = new GetJfrogServersExecutor(build, jfrogServersID);
            getArtifactoryServerExecutor.execute();
            step.jfrogServers = getArtifactoryServerExecutor.getAJfrogServers();
            return step.jfrogServers;
        }

        @Override
        public ArtifactoryServer getUsageReportServer() {
            return Utils.prepareArtifactoryServer(step.getJfrogServersID(), null);
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
            return "Get Jfrog servers from Jenkins config";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}


