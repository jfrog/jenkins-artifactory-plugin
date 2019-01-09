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
}
