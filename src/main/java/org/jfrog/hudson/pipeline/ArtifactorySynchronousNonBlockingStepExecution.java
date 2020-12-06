package org.jfrog.hudson.pipeline;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.IOException;

import static org.jfrog.hudson.pipeline.common.Utils.extractRootWorkspace;

public abstract class ArtifactorySynchronousNonBlockingStepExecution<T> extends SynchronousNonBlockingStepExecution<T> {
    protected static final long serialVersionUID = 1L;

    protected transient TaskListener listener;
    protected transient Launcher launcher;
    protected transient WorkflowRun build;
    // The step's root workspace
    protected transient FilePath rootWs;
    // The step's working directory
    protected transient FilePath ws;
    protected transient EnvVars env;

    protected ArtifactorySynchronousNonBlockingStepExecution(StepContext context) throws IOException, InterruptedException {
        super(context);
        this.listener = context.get(TaskListener.class);
        this.build = context.get(WorkflowRun.class);
        this.launcher = context.get(Launcher.class);
        this.ws = context.get(FilePath.class);
        this.rootWs = extractRootWorkspace(context, this.build, this.ws);
        this.env = context.get(EnvVars.class);
    }
}
