package org.jfrog.hudson.pipeline;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;

public class ScriptedITest extends PipelineITestBase {

    @Test
    public void uploadTest() throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(readPipeline("upload.groovy")));
        WorkflowRun build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("SUCCESS", build);
    }
}