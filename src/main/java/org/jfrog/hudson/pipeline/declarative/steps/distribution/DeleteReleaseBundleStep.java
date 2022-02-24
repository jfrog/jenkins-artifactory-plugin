package org.jfrog.hudson.pipeline.declarative.steps.distribution;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.ReleaseBundleDeleteExecutor;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * @author yahavi
 **/
@SuppressWarnings("unused")
public class DeleteReleaseBundleStep extends RemoteReleaseBundleStep {
    public static final String STEP_NAME = "dsDeleteReleaseBundle";
    private boolean deleteFromDist;

    @DataBoundConstructor
    public DeleteReleaseBundleStep(String serverId, String name, String version) {
        super(serverId, name, version);
    }

    @DataBoundSetter
    public void setCountryCodes(List<String> countryCodes) {
        this.countryCodes = countryCodes;
    }

    @DataBoundSetter
    public void setDeleteFromDist(boolean deleteFromDist) {
        this.deleteFromDist = deleteFromDist;
    }

    @DataBoundSetter
    public void setDistRules(String distRules) {
        this.distRules = distRules;
    }

    @DataBoundSetter
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @DataBoundSetter
    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    @DataBoundSetter
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @DataBoundSetter
    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private final transient DeleteReleaseBundleStep step;

        @Inject
        public Execution(DeleteReleaseBundleStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected Void runStep() throws Exception {
            DistributionServer server = DeclarativePipelineUtils.getDistributionServer(build, rootWs, step.serverId, true);
            new ReleaseBundleDeleteExecutor(server, step.name, step.version, step.dryRun, step.sync, step.deleteFromDist,
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
