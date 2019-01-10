package org.jfrog.hudson.pipeline.integrationTests;

import org.junit.Test;

public class ScriptedITest extends PipelineITestBase {

    public ScriptedITest() {
        super(PipelineType.SCRIPTED);
    }

    @Test
    public void downloadTest() throws Exception {
        super.downloadTest("scripted-download-test");
    }

    @Test
    public void uploadTest() throws Exception {
        super.uploadTest("scripted-upload-test");
    }

    @Test
    public void promotionTest() throws Exception {
        super.promotionTest("scripted-promotion-test");
    }

    @Test
    public void mavenTest() throws Exception {
        buildWorkflowProject("maven");
    }

    @Test
    public void npmTest() throws Exception {
        super.npmTest("scripted-npm-test");
    }
}
