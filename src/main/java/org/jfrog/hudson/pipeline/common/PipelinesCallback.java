package org.jfrog.hudson.pipeline.common;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Send information to JFrog Pipelines after a pipeline job finished.
 * Input parameter:
 * { stepId: <JFrog Pipelines step ID> }
 * Output:
 * {
 * action: "status",
 * status: <jenkins status>,
 * stepId: <JFrog Pipelines step ID>
 * }
 */
@Extension
public class PipelinesCallback extends FlowExecutionListener {
    private static final String JF_PIPELINES_ENV = "JFrogPipelines";
    private static final String STEP_ID_KEY = "stepId";

    @Override
    public void onCompleted(@Nonnull FlowExecution execution) {
        TaskListener listener = null;
        try {
            FlowExecutionOwner owner = execution.getOwner();
            WorkflowRun run = (WorkflowRun) owner.getExecutable();
            listener = owner.getListener();
            EnvVars envVars = run.getEnvironment(listener);
            String data = envVars.get(JF_PIPELINES_ENV);
            if (data == null) {
                return;
            }
            JSONObject jsonObject = JSONObject.fromObject(data);
            String stepId = jsonObject.getString(STEP_ID_KEY);
        } catch (IOException | InterruptedException e) {
            PrintStream outputStream = listener == null ? System.err : listener.getLogger();
            ExceptionUtils.printRootCauseStackTrace(e, outputStream);
        }
    }
}
