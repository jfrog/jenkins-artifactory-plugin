package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.CollectIssuesExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Issues;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class CollectIssuesStep extends AbstractStepImpl {

    private Issues issues;
    private ArtifactoryServer server;
    private String config;

    @DataBoundConstructor
    public CollectIssuesStep(Issues issues, ArtifactoryServer server, String config) {
        this.issues = issues;
        this.server = server;
        this.config = config;
    }

    public Issues getIssues() {
        return issues;
    }

    public String getConfig() {
        return config;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Boolean> {

        private transient CollectIssuesStep step;


        @Inject
        public Execution(CollectIssuesStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Whitelisted
        @Override
        protected Boolean run() throws Exception {
            CollectIssuesExecutor collectIssuesExecutor = new CollectIssuesExecutor(build, listener, ws,
                    step.getIssues().getBuildName(), step.getConfig(), step.getIssues(), step.getServer());
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

