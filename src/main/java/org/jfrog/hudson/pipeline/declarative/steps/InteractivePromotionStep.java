package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.PromotionConfig;
import org.jfrog.hudson.release.promotion.UnifiedPromoteBuildAction;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

@SuppressWarnings("unused")
public class InteractivePromotionStep extends PromoteBuildStep {

    public static final String STEP_NAME = "rtInteractivePromotion";
    private String displayName;

    @DataBoundConstructor
    public InteractivePromotionStep(String serverId, String targetRepo) {
        super(serverId, targetRepo);
    }

    @DataBoundSetter
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient InteractivePromotionStep step;

        @Override
        protected Boolean run() throws Exception {
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(listener, build, ws, getContext(), step.serverId);
            ArtifactoryConfigurator configurator = new ArtifactoryConfigurator(Utils.prepareArtifactoryServer(null, server));
            addPromotionAction(configurator);
            return true;
        }

        private void addPromotionAction(ArtifactoryConfigurator configurator) throws IOException, InterruptedException {
            PromotionConfig pipelinePromotionConfig = step.preparePromotionConfig(getContext());
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
            return "Add Interactive promotion";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
