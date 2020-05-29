package org.jfrog.hudson.pipeline;

import hudson.Extension;
import hudson.model.TaskListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.hudson.PipelinesServer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.jfrog.hudson.PipelinesServer.FAILURE_PREFIX;

@SuppressWarnings("unused")
@Extension
public class PipelinesCallback extends FlowExecutionListener {

    @Override
    public void onCompleted(@Nonnull FlowExecution execution) {
        TaskListener listener = null;
        try {
            listener = getTaskListener(execution);
            WorkflowRun run = getWorkflowRun(execution);
            PipelinesServer.reportJob(run, listener);
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
