package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.executors.NpmPublishExecutor;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings("unused")
public class NpmPublishStep extends AbstractStepImpl {

    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String path;
    private String args;

    @DataBoundConstructor
    public NpmPublishStep(BuildInfo buildInfo, NpmBuild npmBuild, String path, String args) {
        this.buildInfo = buildInfo;
        this.npmBuild = npmBuild;
        this.path = path;
        this.args = args;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient NpmPublishStep step;

        @Override
        protected BuildInfo run() throws Exception {
            return new NpmPublishExecutor(listener, getContext(), step.buildInfo, step.npmBuild, step.args, step.path, ws, build).execute();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmPublishStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNpmPublish";
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory npm publish";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}