package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import hudson.model.ResultTrend;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.jfrog.hudson.jfpipelines.PipelinesHttpClient.MINIMAL_PIPELINES_VERSION;

public class PipelinesHttpClientTest {

    /**
     * Check get version with response code 200.
     */
    @Test
    public void getVersionNormalTest() {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/response-headers?version=" + MINIMAL_PIPELINES_VERSION)) {
            ArtifactoryVersion version = client.getVersion();
            Assert.assertTrue(version.isAtLeast(MINIMAL_PIPELINES_VERSION));
        } catch (IOException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check get version with response code 401.
     */
    @Test(expected = IOException.class)
    public void getVersionUnauthorizedTest() throws IOException {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/status/401")) {
            client.getVersion();
            Assert.fail("Should throw not found exception");
        }
    }

    /**
     * Check get version with response code 404.
     */
    @Test
    public void getVersionWrongCbkTest() {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/status/404")) {
            ArtifactoryVersion version = client.getVersion();
            Assert.assertTrue(version.isNotFound());
        } catch (IOException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check verify version with response code 200 and compatible version.
     */
    @Test
    public void verityVersionNormalTest() {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/response-headers?version=" + MINIMAL_PIPELINES_VERSION)) {
            ArtifactoryVersion version = client.verifyCompatiblePipelinesVersion();
            Assert.assertTrue(version.isAtLeast(MINIMAL_PIPELINES_VERSION));
        } catch (VersionException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check verify version with response code 200 and incompatible version.
     */
    @Test()
    public void verifyVersionIncompatibleTest() {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/response-headers?version=1.5.0")) {
            client.verifyCompatiblePipelinesVersion();
            Assert.fail("Should throw incompatible version exception");
        } catch (VersionException e) {
            Assert.assertEquals(VersionCompatibilityType.INCOMPATIBLE, e.getVersionCompatibilityType());
        }
    }

    /**
     * Check verify version with response code 401.
     */
    @Test
    public void verifyVersionUnauthorizedTest() {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/status/401")) {
            client.verifyCompatiblePipelinesVersion();
            Assert.fail("Should throw not found exception");
        } catch (VersionException e) {
            Assert.assertEquals(VersionCompatibilityType.NOT_FOUND, e.getVersionCompatibilityType());
            Assert.assertTrue(
                    "Exception message should start with 'Error occurred while requesting version information' but was " + e.getMessage(),
                    e.getMessage().startsWith("Error occurred while requesting version information"));
        }
    }

    /**
     * Check verify version with response code 404.
     */
    @Test
    public void verifyVersionWrongCbkTest() {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/status/404")) {
            client.verifyCompatiblePipelinesVersion();
            Assert.fail("Should throw not found exception");
        } catch (VersionException e) {
            Assert.assertEquals(VersionCompatibilityType.NOT_FOUND, e.getVersionCompatibilityType());
            Assert.assertEquals("There is either an incompatible or no instance of Pipelines at the provided URL.", e.getMessage());
        }
    }

    /**
     * Test jobComplete without output resources.
     */
    @Test
    public void jobCompleteTest() {
        try (PipelinesHttpClient client = createClient("http://httpbin.org/post")) {
            HttpResponse response = client.jobCompleted(ResultTrend.SUCCESS, "5", null);
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            try (InputStream content = response.getEntity().getContent()) {
                JsonNode jsonObject = client.getJsonNode(content);
                Assert.assertNotNull(jsonObject);
                JsonNode child = getAndAssertChild(jsonObject, "json", null);

                // Check action == "status"
                getAndAssertChild(child, "action", "status");

                // Check status == "SUCCESS"
                getAndAssertChild(child, "status", ResultTrend.SUCCESS.getID());

                // Check stepId == "5"
                getAndAssertChild(child, "stepId", "5");

                Assert.assertFalse(child.has("outputResources"));
            }
        } catch (IOException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Test jobComplete with output resources.
     */
    @Test
    public void jobCompleteOutputResourcesTest() {
        OutputResource[] outputResources = {
                new OutputResource("resource1", ImmutableMap.of("a", "b")),
                new OutputResource("resource2", ImmutableMap.of("c", "d"))
        };
        try (PipelinesHttpClient client = createClient("http://httpbin.org/post")) {
            HttpResponse response = client.jobCompleted(ResultTrend.SUCCESS, "5", outputResources);
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            try (InputStream content = response.getEntity().getContent()) {
                JsonNode jsonObject = client.getJsonNode(content);
                Assert.assertNotNull(jsonObject);

                JsonNode child = getAndAssertChild(jsonObject, "json", null);
                JsonNode outputResourcesNode = getAndAssertChild(child, "outputResources", null);

                // Check resource1
                JsonNode resource = getAndAssertChild(outputResourcesNode, 0);
                getAndAssertChild(resource, "name", "resource1");
                JsonNode contentNode = getAndAssertChild(resource, "content", null);
                getAndAssertChild(contentNode, "a", "b");

                // Check resource2
                resource = getAndAssertChild(outputResourcesNode, 1);
                getAndAssertChild(resource, "name", "resource2");
                contentNode = getAndAssertChild(resource, "content", null);
                getAndAssertChild(contentNode, "c", "d");
            }
        } catch (IOException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Get JsonNode, assert and return its child.
     *
     * @param jsonNode - The node
     * @param name     - The name of the child
     * @param value    - The value to assert. Can be null if no value.
     * @return the child
     */
    private JsonNode getAndAssertChild(JsonNode jsonNode, String name, String value) {
        JsonNode child = jsonNode.get(name);
        Assert.assertNotNull(child);
        if (value != null) {
            Assert.assertEquals(value, child.textValue());
        }
        return child;
    }

    /**
     * Get JsonNode, assert and return its child.
     *
     * @param jsonNode - The node
     * @param index    - The index of the child
     * @return the child
     */
    private JsonNode getAndAssertChild(JsonNode jsonNode, int index) {
        JsonNode child = jsonNode.get(index);
        Assert.assertNotNull(child);
        return child;
    }

    private PipelinesHttpClient createClient(String pipelinesCbk) {
        return new PipelinesHttpClient(pipelinesCbk, "");
    }
}
