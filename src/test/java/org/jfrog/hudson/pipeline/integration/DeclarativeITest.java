package org.jfrog.hudson.pipeline.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Result;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.StringBody;

import static org.jfrog.hudson.TestUtils.getAndAssertChild;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(1080)) {
            // Run pipeline
            runPipeline("jfPipelinesResources");

            // Get sent request from the mock server
            HttpRequest[] requests = mockServer.retrieveRecordedRequests(null);
            assertEquals(1, ArrayUtils.getLength(requests));
            StringBody body = (StringBody) requests[0].getBody();
            JsonNode requestTree = new ObjectMapper().readTree(body.getValue());

            // Check request content
            getAndAssertChild(requestTree, "action", "status");
            getAndAssertChild(requestTree, "status", Result.SUCCESS.toString());
            getAndAssertChild(requestTree, "stepId", "5");
            JsonNode outputResources = getAndAssertChild(requestTree, "outputResources", null);
            assertEquals(2, outputResources.size());

            // Check output resource 1
            JsonNode resource = getAndAssertChild(outputResources, 0);
            getAndAssertChild(resource, "name", "resource1");
            JsonNode content = getAndAssertChild(resource, "content", null);
            getAndAssertChild(content, "a", "b");

            // Test output resource 2
            resource = getAndAssertChild(outputResources, 1);
            getAndAssertChild(resource, "name", "resource2");
            content = getAndAssertChild(resource, "content", null);
            getAndAssertChild(content, "c", "d");
        }
    }

    @Test
    public void jfPipelinesReportNowTest() throws Exception {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(1080)) {
            // Run pipeline
            runPipeline("jfPipelinesReport");

            // Get sent request from the mock server
            HttpRequest[] requests = mockServer.retrieveRecordedRequests(null);
            assertEquals(1, ArrayUtils.getLength(requests));
            StringBody body = (StringBody) requests[0].getBody();
            JsonNode responseTree = new ObjectMapper().readTree(body.getValue());

            // Check request content
            getAndAssertChild(responseTree, "action", "status");
            getAndAssertChild(responseTree, "status", Result.UNSTABLE.toString());
            getAndAssertChild(responseTree, "stepId", "5");
            assertFalse(responseTree.has("outputResources"));
        }
    }
}
