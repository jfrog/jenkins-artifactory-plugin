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
    public void downloadDuplicationsPart1Test() throws Exception {
        super.downloadDuplicationsTest("scripted:downloadDuplicationsPart1 test", "downloadDuplicationsPart1");
    }

    @Test
    public void downloadDuplicationsPart2Test() throws Exception {
        super.downloadDuplicationsTest("scripted:downloadDuplicationsPart2 test", "downloadDuplicationsPart2");
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
        super.uploadTest("scripted:upload test", null, "upload");
    }

    @Test
    public void uploadDuplicationsPart1Test() throws Exception {
        super.uploadDuplicationsTest("scripted:uploadDuplicationsPart1 test", null, "uploadDuplicationsPart1");
    }

    @Test
    public void uploadDuplicationsPart2Test() throws Exception {
        super.uploadDuplicationsTest("scripted:uploadDuplicationsPart2 test", null, "uploadDuplicationsPart2");
    }

    @Test
    public void platformUploadTest() throws Exception {
        super.uploadTest("scripted:platform upload test", null, "uploadUsingPlatformConfig");
    }

    @Test
    public void uploadWithProjectTest() throws Exception {
        super.uploadTest("scripted:project upload test", "jit", "uploadWithProject");
    }

    @Test
    public void uploadWithPropsTest() throws Exception {
        super.uploadWithPropsTest();
    }

    @Test
    public void uploadDownloadCustomModuleNameTest() throws Exception {
        super.uploadDownloadCustomModuleNameTest("scripted:uploadDownloadCustomModuleName test");
    }

    @Test
    public void promotionTest() throws Exception {
        super.promotionTest("promote", "scripted:promotion test", "");
    }

    @Test
    public void promotionProjectTest() throws Exception {
        super.promotionTest("promoteProject", "scripted:promotionProject test", PROJECT_KEY);
    }

    @Test
    public void mavenTest() throws Exception {
        super.mavenTest("scripted:maven test", false);
    }

    @Test
    public void mavenWrapperTest() throws Exception {
        super.mavenTest("scripted:mavenWrapper test", true);
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
    public void npmInstallTest() throws Exception {
        super.npmTest("npmInstall", "scripted:npm install test", "package-name1:0.0.1");
    }

    @Test
    public void npmCiTest() throws Exception {
        super.npmTest("npmCi", "scripted:npm ci test", "package-name1:0.0.1");
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

    @Test
    public void buildAppendTest() throws Exception {
        super.buildAppendTest("scripted:buildAppend test");
    }

    @Test
    public void rbCreateUpdateSign() throws Exception {
        super.rbCreateUpdateSign("scripted:createUpdateSign");
    }

    @Test
    public void rbCreateDistDel() throws Exception {
        super.rbCreateDistDel("scripted:createDistributeDelete");
    }

    @Test
    public void buildInfoProjects() throws Exception {
        super.buildInfoProjects("scripted:buildInfoProjects");
    }

    @Test
    public void buildRetention() throws Exception {
        super.buildRetention("scripted:buildRetention");
    }
}
