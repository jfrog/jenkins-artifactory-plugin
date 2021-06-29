package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleUpdateExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class UpdateReleaseBundleStep extends CreateUpdateReleaseBundleStep {
    static final String STEP_NAME = "updateReleaseBundle";

    @DataBoundConstructor
    public UpdateReleaseBundleStep(DistributionServer server, String name, String version, String spec,
                                   String storingRepo, boolean signImmediately, boolean dryRun,
                                   String gpgPassphrase, String releaseNotesPath, String releaseNotesSyntax,
                                   String description) {
        super(server, name, version, spec, storingRepo, signImmediately, dryRun, gpgPassphrase, releaseNotesPath, releaseNotesSyntax, description);
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private final transient UpdateReleaseBundleStep step;

        @Inject
        public Execution(UpdateReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            new ReleaseBundleUpdateExecutor(step.getServer(), step.name, step.version, step.getSpec(),
                    step.storingRepo, step.signImmediately, step.dryRun, step.gpgPassphrase,
                    step.releaseNotesPath, step.releaseNotesSyntax, step.description, listener, build, ws, env).execute();
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
            super(UpdateReleaseBundleStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Update a release bundle";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
