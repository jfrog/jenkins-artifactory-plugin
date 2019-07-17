package org.jfrog.hudson.pipeline.integration;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Test;

/**
 * @author yahavi
 */
public class DeclarativeITest extends CommonITestsPipeline {

    public DeclarativeITest() {
        super(PipelineType.DECLARATIVE);
    }

    @Test
    public void downloadByPatternTest() throws Exception {
        super.downloadByPatternTest("declarative:downloadByPattern test");
    }

    @Test
    public void downloadByAqlTest() throws Exception {
        super.downloadByAqlTest("declarative:downloadByAql test");
    }

    @Test
    public void downloadByPatternAndBuildTest() throws Exception {
        super.downloadByPatternAndBuildTest("declarative:downloadByPatternAndBuild test");
    }

    @Test
    public void downloadByBuildOnlyTest() throws Exception {
        super.downloadByBuildOnlyTest("declarative:downloadByBuildOnly test");
    }

    @Test
    public void downloadNonExistingBuildTest() throws Exception {
        super.downloadNonExistingBuildTest("declarative:downloadNonExistingBuild test");
    }

    @Test
    public void downloadByShaAndBuildTest() throws Exception {
        super.downloadByShaAndBuildTest("declarative:downloadByShaAndBuild test");
    }

    @Test
    public void downloadByShaAndBuildNameTest() throws Exception {
        super.downloadByShaAndBuildNameTest("declarative:downloadByShaAndBuildName test");
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

    @Test
    public void setPropsTest() throws Exception {
        super.setPropsTest("declarative:setProps test");
    }

    @Test
    public void deletePropsTest() throws Exception {
        super.deletePropsTest("declarative:deleteProps test");
    }

    @Test
    public void dockerPushTest() throws Exception {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);
        super.dockerPushTest("declarative:dockerPush test");
    }

  @Test
    public void xrayScanFailTrueTest() throws Exception {
        if (!ITestUtils.shouldRunXrayTest()) {
            return;
        }
        super.xrayScanTest("declarative:xrayScanFailBuildTrue test", true);
    }

    @Test
    public void xrayScanFailFalseTest() throws Exception {
        if (!ITestUtils.shouldRunXrayTest()) {
            return;
        }
        super.xrayScanTest("declarative:xrayScanFailBuildFalse test", false);
    }
}
