package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleDeleteExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class DeleteReleaseBundleStep extends AbstractStepImpl {
    static final String STEP_NAME = "deleteReleaseBundle";

    private final DistributionServer server;
    private final List<String> countryCodes;
    private final boolean deleteFromDist;
    private final String distRules;
    private final String siteName;
    private final String cityName;
    private final boolean dryRun;
    private final String version;
    private final boolean sync;
    private final String name;

    @DataBoundConstructor
    public DeleteReleaseBundleStep(DistributionServer server, String name, String version, boolean dryRun, boolean sync,
                                   boolean deleteFromDist, String distRules, List<String> countryCodes,
                                   String siteName, String cityName) {
        this.server = server;
        this.name = name;
        this.version = version;
        this.dryRun = dryRun;
        this.sync = sync;
        this.deleteFromDist = deleteFromDist;
        this.distRules = distRules;
        this.countryCodes = countryCodes;
        this.siteName = siteName;
        this.cityName = cityName;
    }

    public DistributionServer getServer() {
        return server;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

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
