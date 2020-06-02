package org.jfrog.hudson.jfpipelines.listeners;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;

import javax.annotation.Nonnull;

/**
 * This class implements a Jenkins Freestyle, Maven and Ivy job types listener to integrate with JFrog Pipelines.
 */
@SuppressWarnings("unused")
@Extension
public class AbstractBuildListener extends RunListener<AbstractBuild<?, ?>> {

    /**
     * When a Jenkins UI job started, report back status to JFrog pipelines.
     *
     * @param run      - The Jenkins build
     * @param listener - The Jenkins task listener
     */
    @Override
    public void onStarted(AbstractBuild<?, ?> run, TaskListener listener) {
        JFrogPipelinesServer.reportStarted(run, listener);
    }

    /**
     * When a Jenkins UI job completes, report back status to JFrog pipelines.
     *
     * @param run      - The Jenkins build
     * @param listener - The Jenkins task listener
     */
    @Override
    public void onCompleted(AbstractBuild<?, ?> run, @Nonnull TaskListener listener) {
        JFrogPipelinesServer.reportCompleted(run, listener);
    }
}
