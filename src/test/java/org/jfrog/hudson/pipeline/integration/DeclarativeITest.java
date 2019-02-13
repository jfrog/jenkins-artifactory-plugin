package org.jfrog.hudson.pipeline.integration;

import org.junit.Test;

/**
 * @author yahavi
 */
public class DeclarativeITest extends CommonITestsPipeline {

    public DeclarativeITest() {
        super(PipelineType.DECLARATIVE);
    }

    @Test
    public void downloadTest() throws Exception {
        super.downloadTest("declarative:download test");
    }

    @Test
    public void uploadTest() throws Exception {
        super.uploadTest("declarative:upload test");
    }

    @Test
    public void promotionTest() throws Exception {
        super.promotionTest("declarative:promotion test");
    }

    @Test
    public void mavenTest() throws Exception {
        super.mavenTest("declarative:maven test");
    }

    @Test
    public void gradleTest() throws Exception {
        super.gradleTest("declarative:gradle test");
    }

    @Test
    public void gradleCiServerTest() throws Exception {
        super.gradleCiServerTest("declarative:gradle-ci test");
    }

    @Test
    public void npmTest() throws Exception {
        super.npmTest("declarative:npm test");
    }
}
