package org.jfrog.hudson.pipeline.declarative.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleCreateExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author yahavi
 **/
@SuppressWarnings("unused")
public class CreateReleaseBundleStep extends CreateUpdateReleaseBundleStep {
    public static final String STEP_NAME = "dsCreateReleaseBundle";

    @DataBoundConstructor
    public CreateReleaseBundleStep(String serverId, String name, String version, String spec) {
        super(serverId, name, version, spec);
    }

    @DataBoundSetter
    public void setReleaseNotesSyntax(String releaseNotesSyntax) {
        this.releaseNotesSyntax = releaseNotesSyntax;
    }

    @DataBoundSetter
    public void setSignImmediately(boolean signImmediately) {
        this.signImmediately = signImmediately;
    }

    @DataBoundSetter
    public void setReleaseNotesPath(String releaseNotesPath) {
        this.releaseNotesPath = releaseNotesPath;
    }

    @DataBoundSetter
    public void setGpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
    }

    @DataBoundSetter
    public void setStoringRepo(String storingRepo) {
        this.storingRepo = storingRepo;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    @DataBoundSetter
    public void setSpecPath(String specPath) {
        this.specPath = specPath;
    }

    @DataBoundSetter
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<Void> {
protected static final long serialVersionUID = 1L;
        private final transient CreateReleaseBundleStep step;

        @Inject
        public Execution(CreateReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected Void runStep() throws Exception {
            DistributionServer server = DeclarativePipelineUtils.getDistributionServer(build, rootWs, step.serverId, true);
            String spec = getSpec(step.specPath, step.spec);
            new ReleaseBundleCreateExecutor(server, step.name, step.version, spec, step.storingRepo,
                    step.signImmediately, step.dryRun, step.gpgPassphrase, step.releaseNotesPath, step.releaseNotesSyntax,
                    step.description, listener, build, ws, env).execute();
            return null;
        }

        @Override
        public org.jfrog.hudson.ArtifactoryServer getUsageReportServer() throws Exception {
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
            return "Create release bundle";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}