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
    public void uploadDownloadCustomModuleNameTest() throws Exception {
        super.uploadDownloadCustomModuleNameTest("declarative:uploadDownloadCustomModuleName test");
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
    public void gradleCiServerPublicationTest() throws Exception {
        super.gradleCiServerPublicationTest("declarative:gradle-ci-publication test");
    }

    @Test
    public void npmTest() throws Exception {
        super.npmTest("npm", "declarative:npm test", "package-name1:0.0.1");
    }

    @Test
    public void npmCustomModuleNameTest() throws Exception {
        super.npmTest("npmCustomModuleName", "declarative:npmCustomModuleName test", "my-npm-module");
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
    public void conanTest() throws Exception {
        super.conanTest("conan", "declarative:conan test");
    }

    @Test
    public void pipTest() throws Exception {
        super.pipTest("pip", "declarative:pip test", "my-pip-module");
    }

    @Test
    public void nugetTest() throws Exception {
        super.nugetTest("nuget", "declarative:nuget test", "packagesconfig");
    }

    @Test
    public void dotnetTest() throws Exception {
        super.dotnetTest("dotnet", "declarative:dotnet test", "reference");
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
        super.dockerPushTest("declarative:dockerPush test");
    }

    @Test
    public void dockerPullTest() throws Exception {
        super.dockerPullTest("declarative:dockerPull test");
    }

    @Test
    public void xrayScanFailTrueTest() throws Exception {
        super.xrayScanTest("declarative:xrayScanFailBuildTrue test", true);
    }

    @Test
    public void xrayScanFailFalseTest() throws Exception {
        super.xrayScanTest("declarative:xrayScanFailBuildFalse test", false);
    }

    @Test
    public void collectIssuesTest() throws Exception {
        super.collectIssuesTest("declarative:collectIssues test");
    }

    @Test
    public void jfPipelinesOutputResourcesTest() throws Exception {
        super.jfPipelinesOutputResourcesTest();
    }

    @Test
    public void jfPipelinesReportStatusTest() throws Exception {
        super.jfPipelinesReportStatusTest();
    }

    @Test
    public void buildTriggerGlobalServerTest() throws Exception {
        super.buildTriggerGlobalServerTest();
    }

    @Test
    public void buildTriggerNewServerTest() throws Exception {
        super.buildTriggerNewServerTest();
    }

    @Test
    public void buildAppendTest() throws Exception {
        super.buildAppendTest("declarative:buildAppend test");
    }
}
