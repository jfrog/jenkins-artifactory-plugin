package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.PromotionConfig;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.release.promotion.UnifiedPromoteBuildAction;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

@SuppressWarnings("unused")
public class InteractivePromotionStep extends PromoteBuildStep {

    public static final String STEP_NAME = "rtAddInteractivePromotion";
    private String displayName;

    @DataBoundConstructor
    public InteractivePromotionStep(String serverId) {
        super(serverId, "");
    }

    @DataBoundSetter
    public void setTargetRepo(String targetRepo) {
        promotionConfig.setTargetRepo(targetRepo);
    }

    @DataBoundSetter
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public static class Execution extends ArtifactorySynchronousStepExecution<Boolean> {

        private transient InteractivePromotionStep step;

        @Inject
        public Execution(InteractivePromotionStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Boolean run() throws Exception {
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            ArtifactoryConfigurator configurator = new ArtifactoryConfigurator(Utils.prepareArtifactoryServer(null, server));
            addPromotionAction(configurator);
            return true;
        }

        private void addPromotionAction(ArtifactoryConfigurator configurator) {
            PromotionConfig pipelinePromotionConfig = step.preparePromotionConfig(build);
            org.jfrog.hudson.release.promotion.PromotionConfig promotionConfig = Utils.convertPromotionConfig(pipelinePromotionConfig);

            synchronized (build.getActions()) {
                UnifiedPromoteBuildAction action = build.getAction(UnifiedPromoteBuildAction.class);
                if (action == null) {
                    action = new UnifiedPromoteBuildAction(this.build);
                    build.getActions().add(action);
                }
                action.addPromotionCandidate(promotionConfig, configurator, step.displayName);
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(InteractivePromotionStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
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
