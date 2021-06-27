package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleUpdateExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class UpdateReleaseBundleStep extends AbstractStepImpl {
    static final String STEP_NAME = "updateReleaseBundle";

    private final DistributionServer server;
    private final String releaseNotesSyntax;
    private final boolean signImmediately;
    private final String releaseNotesPath;
    private final String gpgPassphrase;
    private final String storingRepo;
    private final String description;
    private final boolean dryRun;
    private final String version;
    private final String name;
    private final String spec;

    @DataBoundConstructor
    public UpdateReleaseBundleStep(DistributionServer server, String name, String version, String spec,
                                   String storingRepo, boolean signImmediately, boolean dryRun,
                                   String gpgPassphrase, String releaseNotesPath, String releaseNotesSyntax,
                                   String description) {
        this.server = server;
        this.name = name;
        this.version = version;
        this.spec = spec;
        this.storingRepo = storingRepo;
        this.signImmediately = signImmediately;
        this.dryRun = dryRun;
        this.gpgPassphrase = gpgPassphrase;
        this.releaseNotesPath = releaseNotesPath;
        this.releaseNotesSyntax = releaseNotesSyntax;
        this.description = description;
    }

    public String getSpec() {
        return spec;
    }

    public DistributionServer getServer() {
        return server;
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
