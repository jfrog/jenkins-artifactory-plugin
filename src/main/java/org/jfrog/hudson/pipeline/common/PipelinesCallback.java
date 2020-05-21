package org.jfrog.hudson.pipeline.common;

import hudson.EnvVars;
import hudson.Extension;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class PipelinesCallback extends FlowExecutionListener {
    private static final String JF_PIPELINES_ENV = "JF_PIPELINES";

    @Override
    public void onCompleted(@Nonnull FlowExecution execution) {
        try {
            FlowExecutionOwner owner = execution.getOwner();
            WorkflowRun run = (WorkflowRun) owner.getExecutable();
            EnvVars envVars = run.getEnvironment(owner.getListener());
            String data = envVars.get(JF_PIPELINES_ENV);
        } catch (IOException | InterruptedException e) {
            ExceptionUtils.printRootCauseStackTrace(e);
        }
    }
}
