package org.jfrog.hudson.pipeline.integration;

import org.junit.Test;

public class ScriptedDockerITest extends CommonITestsPipeline {

    public ScriptedDockerITest() {
        super(PipelineType.SCRIPTED);
    }

    @Test
    public void mavenJibTest() throws Exception {
        super.mavenJibTest("scripted:mavenJib test");
    }

    @Test
    public void dockerPushTest() throws Exception {
        super.dockerPushTest("scripted:dockerPush test");
    }

    @Test
    public void dockerPullTest() throws Exception {
        super.dockerPullTest("scripted:dockerPull test");
    }

    @Test
    public void goTest() throws Exception {
        super.goTest("go", "scripted:go test", "github.com/you/hello");
    }

    @Test
    public void goCustomModuleNameTest() throws Exception {
        super.goTest("goCustomModuleName", "scripted:goCustomModuleName test", "my-Go-module");
    }

    @Test
    public void xrayScanFailTrueTest() throws Exception {
        super.xrayScanTest("scripted:xrayScanFailBuildTrue test", true, false);
    }

    @Test
    public void xrayScanFailFalseTest() throws Exception {
        super.xrayScanTest("scripted:xrayScanFailBuildFalse test", false, true);
    }

}
