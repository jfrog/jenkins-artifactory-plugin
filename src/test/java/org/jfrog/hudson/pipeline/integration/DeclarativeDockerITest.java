package org.jfrog.hudson.pipeline.integration;

import org.junit.Test;

public class DeclarativeDockerITest extends CommonITestsPipeline {

    DeclarativeDockerITest() {
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

}
