package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleSignExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class SignReleaseBundleStep extends AbstractStepImpl {
    static final String STEP_NAME = "signReleaseBundle";

    private final DistributionServer server;
    private final String gpgPassphrase;
    private final String storingRepo;
    private final String version;
    private final String name;
    private final String spec;

    @DataBoundConstructor
    public SignReleaseBundleStep(DistributionServer server, String name, String version, String spec,
                                 String gpgPassphrase, String storingRepo) {
        this.server = server;
        this.name = name;
        this.version = version;
        this.spec = spec;
        this.gpgPassphrase = gpgPassphrase;
        this.storingRepo = storingRepo;
    }

    public String getSpec() {
        return spec;
    }

    public DistributionServer getServer() {
        return server;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {
        protected static final long serialVersionUID = 1L;
        private final transient SignReleaseBundleStep step;

        @Inject
        public Execution(SignReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            new ReleaseBundleSignExecutor(step.getServer(), step.name, step.version, step.gpgPassphrase, step.storingRepo,
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
