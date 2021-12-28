package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleDeleteExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class DeleteReleaseBundleStep extends RemoteReleaseBundleStep {
    static final String STEP_NAME = "deleteReleaseBundle";
    private final boolean deleteFromDist;

    @DataBoundConstructor
    public DeleteReleaseBundleStep(DistributionServer server, String name, String version, boolean dryRun, boolean sync,
                                   boolean deleteFromDist, String distRules, List<String> countryCodes,
                                   String siteName, String cityName) {
        super(server, name, version, dryRun, sync, distRules, countryCodes, siteName, cityName);
        this.deleteFromDist = deleteFromDist;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {
protected static final long serialVersionUID = 1L;
        private final transient DeleteReleaseBundleStep step;

        @Inject
        public Execution(DeleteReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void runStep() throws Exception {
            new ReleaseBundleDeleteExecutor(step.getServer(), step.name, step.version, step.dryRun, step.sync, step.deleteFromDist,
                    step.distRules, step.countryCodes, step.siteName, step.cityName, listener, build, ws).execute();
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
            super(DeleteReleaseBundleStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Delete a release bundle";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
