package org.jfrog.hudson.pipeline.integration;

import org.junit.Test;

public class ScriptedDockerITest extends CommonITestsPipeline {

    ScriptedDockerITest() {
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

}
