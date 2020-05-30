package org.jfrog.hudson.jfpipelines;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.jfrog.hudson.PipelinesServer;

import javax.annotation.Nonnull;

/**
 * This class implements a Jenkins Freestyle, Maven and Ivy job types listener to integrate with JFrog Pipelines.
 */
@SuppressWarnings({"unused", "rawtypes"})
@Extension
public class PipelinesRunListener extends RunListener<AbstractBuild> {

    /**
     * When a Jenkins UI job completes, report back status to JFrog pipelines.
     *
     * @param run      - The Jenkins build
     * @param listener - The Jenkins task listener
     */
    @Override
    public void onCompleted(AbstractBuild run, @Nonnull TaskListener listener) {
        PipelinesServer.reportJob(run, listener);
    }
}
