package org.jfrog.hudson.pipeline.declarative.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleUpdateExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author yahavi
 **/
public class UpdateReleaseBundleStep extends AbstractStepImpl {
    public static final String STEP_NAME = "dsUpdateReleaseBundle";
    final String serverId;
    final String version;
    final String name;
    final String spec;

    String releaseNotesSyntax;
    boolean signImmediately;
    String releaseNotesPath;
    String gpgPassphrase;
    String storingRepo;
    String description;
    String specPath;
    boolean dryRun;

    @DataBoundConstructor
    public UpdateReleaseBundleStep(String serverId, String name, String version, String spec) {
        this.serverId = serverId;
        this.name = name;
        this.version = version;
        this.spec = spec;
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

        private final transient UpdateReleaseBundleStep step;

        @Inject
        public Execution(UpdateReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected Void runStep() throws Exception {
            DistributionServer server = DeclarativePipelineUtils.getDistributionServer(build, rootWs, step.serverId, true);
            String spec = getSpec(step.specPath, step.spec);
            new ReleaseBundleUpdateExecutor(server, step.name, step.version, spec, step.storingRepo,
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

    static String getSpec(String specPath, String specParameter) throws IOException {
        if (StringUtils.isNotBlank(specPath)) {
            return FileUtils.readFileToString(new File(specPath), StandardCharsets.UTF_8);
        }
        return specParameter;
    }
}