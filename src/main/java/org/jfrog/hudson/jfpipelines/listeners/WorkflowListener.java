package org.jfrog.hudson.jfpipelines.listeners;

import hudson.Extension;
import hudson.model.TaskListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.jfrog.hudson.jfpipelines.JFrogPipelinesServer.FAILURE_PREFIX;

/**
 * This class implements a Jenkins pipelines jobs listener to integrate with JFrog Pipelines.
 */
@SuppressWarnings("unused")
@Extension
public class WorkflowListener extends FlowExecutionListener {

    /**
     * When a Jenkins pipeline started, report back status to JFrog Pipelines.
     *
     * @param execution - The Jenkins pipeline execution
     */
    @Override
    public void onRunning(@Nonnull FlowExecution execution) {
        reportStatus(execution, true);
    }

    /**
     * When a Jenkins pipeline completes, report back status to JFrog Pipelines.
     *
     * @param execution - The Jenkins pipeline execution
     */
    @Override
    public void onCompleted(@Nonnull FlowExecution execution) {
        reportStatus(execution, false);
    }

    private void reportStatus(@Nonnull FlowExecution execution, boolean started) {
        TaskListener listener = null;
        try {
            listener = getTaskListener(execution);
            WorkflowRun run = getWorkflowRun(execution);
            if (started) {
                JFrogPipelinesServer.reportStarted(run, listener);
            } else {
                JFrogPipelinesServer.reportCompleted(run, listener);
            }
        } catch (IOException e) {
            if (listener != null) {
                // Error on retrieving the WorkflowRun
                new JenkinsBuildInfoLog(listener).error(FAILURE_PREFIX + ExceptionUtils.getRootCauseMessage(e), e);
            } else {
                // Error on retrieving the TaskListener
                System.err.println(FAILURE_PREFIX);
                ExceptionUtils.printRootCauseStackTrace(e);
            }
        }
    }

    private TaskListener getTaskListener(FlowExecution execution) throws IOException {
        return execution.getOwner().getListener();
    }

    private WorkflowRun getWorkflowRun(FlowExecution execution) throws IOException {
        return (WorkflowRun) execution.getOwner().getExecutable();
    }
}
