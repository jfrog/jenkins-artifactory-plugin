package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.PromotionExecutor;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.PromotionConfig;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.HashMap;
import java.util.Map;

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

    public static class Execution extends AbstractSynchronousStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient PromoteBuildStep step;

        @Override
        protected Boolean run() throws Exception {
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

            new PromotionExecutor(Utils.prepareArtifactoryServer(null, step.getServer()), build, listener, getContext(), promotionConfig).execution();
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
        public Map<String, Object> defineArguments(Step step) throws UnsupportedOperationException {
            return new HashMap<String, Object>();
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}
