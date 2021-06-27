package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.GetJFrogPlatformInstancesExecutor;
import org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class GetJFrogPlatformInstanceStep extends AbstractStepImpl {
    static final String STEP_NAME = "getJFrogPlatformInstance";
    private final String instanceId;

    @DataBoundConstructor
    public GetJFrogPlatformInstanceStep(String instanceId) {
        this.instanceId = instanceId;
    }

    private String getInstanceId() {
        return instanceId;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<JFrogPlatformInstance> {

        private transient final GetJFrogPlatformInstanceStep step;

        @Inject
        public Execution(GetJFrogPlatformInstanceStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected JFrogPlatformInstance runStep() throws Exception {
            String JFrogPlatformInstanceID = step.getInstanceId();
            GetJFrogPlatformInstancesExecutor getArtifactoryServerExecutor = new GetJFrogPlatformInstancesExecutor(build, JFrogPlatformInstanceID);
            getArtifactoryServerExecutor.execute();
            return getArtifactoryServerExecutor.getJFrogPlatformInstance();
        }

        @Override
        public ArtifactoryServer getUsageReportServer() {
            return Utils.prepareArtifactoryServer(step.getInstanceId(), null);
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

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Get JFrog Platform instance from Jenkins config";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}


