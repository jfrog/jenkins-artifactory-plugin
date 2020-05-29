package org.jfrog.hudson;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;

/**
 * This class implements a UI jobs listener.
 */
@SuppressWarnings({"unused", "rawtypes"})
@Extension
public class AbstractBuildListener extends RunListener<AbstractBuild> {

    @Override
    public void onCompleted(AbstractBuild run, @Nonnull TaskListener listener) {
        PipelinesServer.reportJob(run, listener);
    }
}
