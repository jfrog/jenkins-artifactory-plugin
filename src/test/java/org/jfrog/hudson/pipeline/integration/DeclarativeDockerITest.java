package org.jfrog.hudson.pipeline.integration;

import org.junit.Test;

public class DeclarativeDockerITest extends CommonITestsPipeline {

    public DeclarativeDockerITest() {
        super(PipelineType.DECLARATIVE);
    }

    @Test
    public void mavenJibTest() throws Exception {
        super.mavenJibTest("declarative:mavenJib test");
    }

    @Test
    public void dockerPushTest() throws Exception {
        super.dockerPushTest("declarative:dockerPush test");
    }

    @Test
    public void dockerPullTest() throws Exception {
        super.dockerPullTest("declarative:dockerPull test");
    }

    @Test
    public void goTest() throws Exception {
        super.goTest("go", "declarative:go test", "github.com/you/hello");
    }

    @Test
    public void goCustomModuleNameTest() throws Exception {
        super.goTest("goCustomModuleName", "declarative:goCustomModuleName test", "my-Go-module");
    }

    @Test
    public void xrayScanFailTrueTest() throws Exception {
        super.xrayScanTest("declarative:xrayScanFailBuildTrue test", true, false);
    }

    @Test
    public void xrayScanFailFalseTest() throws Exception {
        super.xrayScanTest("declarative:xrayScanFailBuildFalse test", false, true);
    }

}
