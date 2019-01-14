package org.jfrog.hudson.pipeline.integrationtests;

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
        super.mavenTest("scripted-maven-test");
    }

    @Test
    public void gradleTest() throws Exception {
        super.gradleTest("scripted-gradle-test");
    }

    @Test
    public void gradleCiServerTest() throws Exception {
        super.gradleCiServerTest("scripted-gradle-ci-test");
    }

    @Test
    public void npmTest() throws Exception {
        super.npmTest("scripted-npm-test");
    }
}
