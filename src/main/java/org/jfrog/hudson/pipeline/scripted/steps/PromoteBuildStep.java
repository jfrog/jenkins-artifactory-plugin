package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.PromotionExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.PromotionConfig;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class PromoteBuildStep extends AbstractStepImpl {

    private ArtifactoryServer server;
    private PromotionConfig promotionConfig;

    @DataBoundConstructor
    public PromoteBuildStep(PromotionConfig promotionConfig, ArtifactoryServer server) {
        this.promotionConfig = promotionConfig;
        this.server = server;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public PromotionConfig getPromotionConfig() {
        return promotionConfig;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {

        private transient PromoteBuildStep step;

        @Inject
        public Execution(PromoteBuildStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean runStep() throws Exception {
            PromotionConfig promotionConfig = step.getPromotionConfig();

            if (StringUtils.isEmpty(promotionConfig.getBuildName())) {
                getContext().onFailure(new MissingArgumentException("Promotion build name is mandatory"));
                return false;
            }

            if (StringUtils.isEmpty(promotionConfig.getBuildNumber())) {
                getContext().onFailure(new MissingArgumentException("Promotion build number is mandatory"));
                return false;
            }

            if (StringUtils.isEmpty(promotionConfig.getTargetRepo())) {
                getContext().onFailure(new MissingArgumentException("Promotion target repository is mandatory"));
                return false;
            }

            new PromotionExecutor(Utils.prepareArtifactoryServer(null, step.getServer()), build, listener, getContext(), promotionConfig).execute();
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(PromoteBuildStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return "artifactoryPromoteBuild";
        }

        @Override
        public String getDisplayName() {
            return "Promote build";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}
