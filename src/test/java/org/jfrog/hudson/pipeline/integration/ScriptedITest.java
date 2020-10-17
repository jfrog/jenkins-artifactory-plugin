package org.jfrog.hudson.pipeline.integration;

import org.junit.Test;

/**
 * @author yahavi
 */
public class ScriptedITest extends CommonITestsPipeline {

    public ScriptedITest() {
        super(PipelineType.SCRIPTED);
    }

    @Test
    public void downloadByPatternTest() throws Exception {
        super.downloadByPatternTest("scripted:downloadByPattern test");
    }

    @Test
    public void downloadByAqlTest() throws Exception {
        super.downloadByAqlTest("scripted:downloadByAql test");
    }

    @Test
    public void downloadByPatternAndBuildTest() throws Exception {
        super.downloadByPatternAndBuildTest("scripted:downloadByPatternAndBuild test");
    }

    @Test
    public void downloadByBuildOnlyTest() throws Exception {
        super.downloadByBuildOnlyTest("scripted:downloadByBuildOnly test");
    }

    @Test
    public void downloadNonExistingBuildTest() throws Exception {
        super.downloadNonExistingBuildTest("scripted:downloadNonExistingBuild test");
    }

    @Test
    public void downloadByShaAndBuildTest() throws Exception {
        super.downloadByShaAndBuildTest("scripted:downloadByShaAndBuild test");
    }

    @Test
    public void downloadByShaAndBuildNameTest() throws Exception {
        super.downloadByShaAndBuildNameTest("scripted:downloadByShaAndBuildName test");
    }

    @Test
    public void uploadTest() throws Exception {
        super.uploadTest("scripted:upload test");
    }

    @Test
    public void uploadDownloadCustomModuleNameTest() throws Exception {
        super.uploadDownloadCustomModuleNameTest("scripted:uploadDownloadCustomModuleName test");
    }

    @Test
    public void promotionTest() throws Exception {
        super.promotionTest("scripted:promotion test");
    }

    @Test
    public void mavenTest() throws Exception {
        super.mavenTest("scripted:maven test");
    }

    @Test
    public void gradleTest() throws Exception {
        super.gradleTest("scripted:gradle test");
    }

    @Test
    public void gradleCiServerTest() throws Exception {
        super.gradleCiServerTest("scripted:gradle-ci test");
    }

    @Test
    public void gradleCiServerPublicationTest() throws Exception {
        super.gradleCiServerPublicationTest("scripted:gradle-ci-publication test");
    }

    @Test
    public void npmTest() throws Exception {
        super.npmTest("npm", "scripted:npm test", "package-name1:0.0.1");
    }

    @Test
    public void npmCustomModuleNameTest() throws Exception {
        super.npmTest("npmCustomModuleName", "scripted:npmCustomModuleName test", "my-npm-module");
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
    public void conanTest() throws Exception {
        super.conanTest("conan", "scripted:conan test");
    }

    @Test
    public void pipTest() throws Exception {
        super.pipTest("pip", "scripted:pip test", "my-pip-module");
    }

    @Test
    public void nugetTest() throws Exception {
        super.nugetTest("nuget", "scripted:nuget test", "packagesconfig");
    }

    @Test
    public void dotnetTest() throws Exception {
        super.dotnetTest("dotnet", "scripted:dotnet test", "reference");
    }

    @Test
    public void setPropsTest() throws Exception {
        super.setPropsTest("scripted:setProps test");
    }

    @Test
    public void deletePropsTest() throws Exception {
        super.deletePropsTest("scripted:deleteProps test");
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
    public void xrayScanFailTrueTest() throws Exception {
        super.xrayScanTest("scripted:xrayScanFailBuildTrue test", true);
    }

    @Test
    public void xrayScanFailFalseTest() throws Exception {
        super.xrayScanTest("scripted:xrayScanFailBuildFalse test", false);
    }

    @Test
    public void collectIssuesTest() throws Exception {
        super.collectIssuesTest("scripted:collectIssues test");
    }

    @Test
    public void appendBuildInfoTest() throws Exception {
        super.appendBuildInfoTest("scripted:appendBuildInfo test");
    }

    @Test
    public void buildTriggerGlobalServerTest() throws Exception {
        super.buildTriggerGlobalServerTest();
    }

    @Test
    public void buildTriggerNewServerTest() throws Exception {
        super.buildTriggerNewServerTest();
    }
}
