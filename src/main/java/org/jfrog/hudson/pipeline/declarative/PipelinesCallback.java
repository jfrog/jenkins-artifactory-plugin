package org.jfrog.hudson.pipeline.declarative;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.hudson.PipelinesServer;
import org.jfrog.hudson.jfpipelines.JfrogPipelinesParam;
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
            EnvVars envVars = getEnvVars(execution, listener);
            JfrogPipelinesParam jfrogPipelinesParam = JfrogPipelinesParam.createFromEnv(envVars);
            if (jfrogPipelinesParam == null) {
                // JFrogPipelines parameter is not set
                return;
            }
            String stepId = jfrogPipelinesParam.getStepId();
            PipelinesServer pipelinesServer = PipelinesServer.getPipelinesServer();
            if (pipelinesServer.isReported(stepId)) {
                pipelinesServer.clearReported(stepId);
                logger.info("Skipping reporting to JFrog Pipelines - status is already reported in jfPipelines step.");
                return;
            }
            pipelinesServer.jobCompleted(getResult(execution), jfrogPipelinesParam.getStepId());
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

    private EnvVars getEnvVars(FlowExecution execution, TaskListener listener) throws IOException, InterruptedException {
        WorkflowRun run = (WorkflowRun) execution.getOwner().getExecutable();
        return run.getEnvironment(listener);
    }

    private Result getResult(FlowExecution execution) throws IOException {
        WorkflowRun run = (WorkflowRun) execution.getOwner().getExecutable();
        return run.getResult();
    }
}
