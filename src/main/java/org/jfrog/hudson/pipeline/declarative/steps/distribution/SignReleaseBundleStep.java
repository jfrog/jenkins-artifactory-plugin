package org.jfrog.hudson.pipeline.declarative.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleSignExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author yahavi
 **/
public class SignReleaseBundleStep extends AbstractStepImpl {
    public static final String STEP_NAME = "dsSignReleaseBundle";
    private final String serverId;
    private final String version;
    private final String name;

    private String gpgPassphrase;
    private String storingRepo;

    @DataBoundConstructor
    public SignReleaseBundleStep(String serverId, String name, String version) {
        this.serverId = serverId;
        this.name = name;
        this.version = version;
    }

    @DataBoundSetter
    public void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
    }

    @DataBoundSetter
    public void setStoringRepo(String storingRepo) {
        this.storingRepo = storingRepo;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private final transient SignReleaseBundleStep step;

        @Inject
        public Execution(SignReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected Void runStep() throws Exception {
            DistributionServer server = DeclarativePipelineUtils.getDistributionServer(build, rootWs, step.serverId, true);
            new ReleaseBundleSignExecutor(server, step.name, step.version, step.gpgPassphrase, step.storingRepo,
                    listener, build, ws).execute();
            return null;
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() {
            return null;
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(SignReleaseBundleStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Sign a release bundle";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
