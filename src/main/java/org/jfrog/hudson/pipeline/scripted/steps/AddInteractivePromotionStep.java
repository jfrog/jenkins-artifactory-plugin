package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.PromotionConfig;
import org.jfrog.hudson.release.promotion.UnifiedPromoteBuildAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by yahavi on 13/03/2017.
 */

public class AddInteractivePromotionStep extends AbstractStepImpl {
    private ArtifactoryServer server;
    private PromotionConfig promotionConfig;
    private String displayName;

    @DataBoundConstructor
    public AddInteractivePromotionStep(PromotionConfig promotionConfig, ArtifactoryServer server, String displayName) {
        this.promotionConfig = promotionConfig;
        this.server = server;
        this.displayName = displayName;
    }

    public ArtifactoryServer getServer() {
        return this.server;
    }

    public PromotionConfig getPromotionConfig() {
        return this.promotionConfig;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {

        private transient AddInteractivePromotionStep step;

        @Inject
        public Execution(AddInteractivePromotionStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean run() throws Exception {
            ArtifactoryConfigurator configurator = new ArtifactoryConfigurator(Utils.prepareArtifactoryServer(null, step.getServer()));
            addPromotionAction(configurator);
            return true;
        }

        private void addPromotionAction(ArtifactoryConfigurator configurator) {
            PromotionConfig pipelinePromotionConfig = step.getPromotionConfig();
            org.jfrog.hudson.release.promotion.PromotionConfig promotionConfig = Utils.convertPromotionConfig(pipelinePromotionConfig);

            synchronized (build.getActions()) {
                UnifiedPromoteBuildAction action = build.getAction(UnifiedPromoteBuildAction.class);
                if (action == null) {
                    action = new UnifiedPromoteBuildAction(this.build);
                    build.getActions().add(action);
                }
                action.addPromotionCandidate(promotionConfig, configurator, step.getDisplayName());
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "addInteractivePromotion";
        }

        @Override
        public String getDisplayName() {
            return "Add interactive promotion";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
