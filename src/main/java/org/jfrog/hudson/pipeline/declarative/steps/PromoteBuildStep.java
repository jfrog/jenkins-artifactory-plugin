package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.PromotionExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.PromotionConfig;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@SuppressWarnings("unused")
public class PromoteBuildStep extends AbstractStepImpl {

    public static final String STEP_NAME = "rtPromote";
    protected PromotionConfig promotionConfig;
    protected String serverId;

    @DataBoundConstructor
    public PromoteBuildStep(String serverId, String targetRepo) {
        this.serverId = serverId;
        promotionConfig = new PromotionConfig();
        if (StringUtils.isNotBlank(targetRepo)) {
            promotionConfig.setTargetRepo(targetRepo);
        }
    }

    @DataBoundSetter
    public void setBuildName(String buildName) {
        promotionConfig.setBuildName(buildName);
    }

    @DataBoundSetter
    public void setBuildNumber(String buildNumber) {
        promotionConfig.setBuildNumber(buildNumber);
    }

    @DataBoundSetter
    public void setSourceRepo(String sourceRepo) {
        promotionConfig.setSourceRepo(sourceRepo);
    }

    @DataBoundSetter
    public void setComment(String comment) {
        promotionConfig.setComment(comment);
    }

    @DataBoundSetter
    public void setStatus(String status) {
        promotionConfig.setStatus(status);
    }

    @DataBoundSetter
    public void setCopy(boolean copy) {
        promotionConfig.setCopy(copy);
    }

    @DataBoundSetter
    public void setIncludeDependencies(boolean includeDependencies) {
        promotionConfig.setIncludeDependencies(includeDependencies);
    }

    @DataBoundSetter
    public void setFailFast(boolean failFast) {
        promotionConfig.setFailFast(failFast);
    }

    PromotionConfig preparePromotionConfig(Run build) {
        if (StringUtils.isBlank(promotionConfig.getBuildName())) {
            promotionConfig.setBuildName(BuildUniqueIdentifierHelper.getBuildName(build));
        }
        if (StringUtils.isBlank(promotionConfig.getBuildNumber())) {
            promotionConfig.setBuildNumber(BuildUniqueIdentifierHelper.getBuildNumber(build));
        }
        return promotionConfig;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient PromoteBuildStep step;

        @Override
        protected Void run() throws Exception {
            PromotionConfig promotionConfig = step.preparePromotionConfig(build);
            ArtifactoryServer server = DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), step.serverId);
            new PromotionExecutor(Utils.prepareArtifactoryServer(null, server), build, listener, getContext(), promotionConfig).execute();
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(PromoteBuildStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
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