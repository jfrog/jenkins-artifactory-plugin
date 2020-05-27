package org.jfrog.hudson.pipeline.common;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.hudson.PipelinesServer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import javax.annotation.Nonnull;
import java.io.IOException;

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
@SuppressWarnings("unused")
@Extension
public class PipelinesCallback extends FlowExecutionListener {
    private static final String JF_PIPELINES_ENV = "JFrogPipelines";
    private static final String STEP_ID_KEY = "stepId";

    @Override
    public void onCompleted(@Nonnull FlowExecution execution) {
        JenkinsBuildInfoLog logger = null;
        try {
            TaskListener listener = getTaskListener(execution);
            logger = new JenkinsBuildInfoLog(listener);
            String data = getJFPipelinesEnv(execution, listener);
            if (data == null) {
                // JFrogPipelines parameter is not set
                return;
            }
            JSONObject jsonObject = JSONObject.fromObject(data);
            String stepId = jsonObject.getString(STEP_ID_KEY);

            PipelinesServer pipelinesServer = PipelinesServer.getPipelinesServer();
            if (pipelinesServer == null) {
                logger.error("Please set JFrog Pipelines server under 'Manage Jenkins' -> 'Configure System' -> 'Pipelines server'.");
                return;
            }
            pipelinesServer.jobComplete(getResult(execution), stepId, null);
        } catch (IOException | InterruptedException e) {
            if (logger != null) {
                logger.error(ExceptionUtils.getRootCauseMessage(e), e);
            } else {
                ExceptionUtils.printRootCauseStackTrace(e);
            }
        }
    }

    private TaskListener getTaskListener(FlowExecution execution) throws IOException {
        return execution.getOwner().getListener();
    }

    private String getJFPipelinesEnv(FlowExecution execution, TaskListener listener) throws IOException, InterruptedException {
        WorkflowRun run = (WorkflowRun) execution.getOwner().getExecutable();
        EnvVars envVars = run.getEnvironment(listener);
        return envVars.get(JF_PIPELINES_ENV);
    }

    private Result getResult(FlowExecution execution) throws IOException {
        WorkflowRun run = (WorkflowRun) execution.getOwner().getExecutable();
        return run.getResult();
    }
}
