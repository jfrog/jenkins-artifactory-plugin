package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleCreateExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class CreateReleaseBundleStep extends CreateUpdateReleaseBundleStep {
    static final String STEP_NAME = "createReleaseBundle";

    @DataBoundConstructor
    public CreateReleaseBundleStep(DistributionServer server, String name, String version, String spec,
                                   String storingRepo, boolean signImmediately, boolean dryRun,
                                   String gpgPassphrase, String releaseNotesPath, String releaseNotesSyntax,
                                   String description) {
        super(server, name, version, spec, storingRepo, signImmediately, dryRun, gpgPassphrase, releaseNotesPath, releaseNotesSyntax, description);
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {
protected static final long serialVersionUID = 1L;
        private final transient CreateReleaseBundleStep step;

        @Inject
        public Execution(CreateReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            new ReleaseBundleCreateExecutor(step.getServer(), step.name, step.version, step.getSpec(),
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
            super(CreateReleaseBundleStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Create a release bundle ";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
