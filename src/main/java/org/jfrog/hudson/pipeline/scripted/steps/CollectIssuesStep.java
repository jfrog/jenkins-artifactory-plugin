package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.executors.CollectIssuesExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.TrackedIssues;
import org.kohsuke.stapler.DataBoundConstructor;

@SuppressWarnings("unused")
public class CollectIssuesStep extends AbstractStepImpl {

    private TrackedIssues trackedIssues;
    private String config;

    @DataBoundConstructor
    public CollectIssuesStep(TrackedIssues trackedIssues, String config) {
        this.trackedIssues = trackedIssues;
        this.config = config;
    }

    public TrackedIssues getTrackedIssues() {
        return trackedIssues;
    }

    public String getConfig() {
        return config;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient CollectIssuesStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Whitelisted
        @Override
        protected Boolean run() throws Exception {
            CollectIssuesExecutor collectIssuesExecutor = new CollectIssuesExecutor(build, listener, ws,
                    step.getTrackedIssues().getBuildName(), step.getConfig(), step.getTrackedIssues());
            collectIssuesExecutor.execute();
            return true;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CollectIssuesStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "collectIssues";
        }

        @Override
        public String getDisplayName() {
            return "Collect issues from git and add them to a build";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }

}

