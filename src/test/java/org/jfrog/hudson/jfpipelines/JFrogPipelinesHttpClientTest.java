package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.databind.JsonNode;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jfrog.build.client.Version;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;
import org.jfrog.hudson.jfpipelines.payloads.JobStatusPayload;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.hudson.TestUtils.getAndAssertChild;
import static org.jfrog.hudson.jfpipelines.JFrogPipelinesHttpClient.MINIMAL_PIPELINES_VERSION;
import static org.jfrog.hudson.util.SerializationUtils.createMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * In this class we send HTTP requests to 'http://httpbin.org' in order to examine the requests.
 * For example - 'GET' request to 'http://httpbin.org/status/401' will return response code 401.
 */
public class JFrogPipelinesHttpClientTest {

    /**
     * Check get version with response code 200.
     */
    @Test
    public void getVersionNormalTest() {
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/response-headers?version=" + MINIMAL_PIPELINES_VERSION)) {
            Version version = client.getVersion();
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
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/status/401")) {
            client.getVersion();
            Assert.fail("Should throw not found exception");
        }
    }

    /**
     * Check get version with response code 404.
     */
    @Test
    public void getVersionWrongUrlTest() {
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/status/404")) {
            Version version = client.getVersion();
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
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/response-headers?version=" + MINIMAL_PIPELINES_VERSION)) {
            Version version = client.verifyCompatibleVersion();
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
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/response-headers?version=1.5.0")) {
            client.verifyCompatibleVersion();
            Assert.fail("Should throw incompatible version exception");
        } catch (VersionException e) {
            assertEquals(VersionCompatibilityType.INCOMPATIBLE, e.getVersionCompatibilityType());
        }
    }

    /**
     * Check verify version with response code 401.
     */
    @Test
    public void verifyVersionUnauthorizedTest() {
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/status/401")) {
            client.verifyCompatibleVersion();
            Assert.fail("Should throw not found exception");
        } catch (VersionException e) {
            assertEquals(VersionCompatibilityType.NOT_FOUND, e.getVersionCompatibilityType());
            Assert.assertTrue(
                    "Exception message should start with 'Error occurred while requesting version information' but was " + e.getMessage(),
                    e.getMessage().startsWith("Error occurred while requesting version information"));
        }
    }

    /**
     * Check verify version with response code 404.
     */
    @Test
    public void verifyVersionWrongUrlTest() {
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/status/404")) {
            client.verifyCompatibleVersion();
            Assert.fail("Should throw not found exception");
        } catch (VersionException e) {
            assertEquals(VersionCompatibilityType.NOT_FOUND, e.getVersionCompatibilityType());
            assertEquals("There is either an incompatible version or no instance of JFrog Pipelines accessible at the provided URL.", e.getMessage());
        }
    }

    /**
     * Test sendStatus without output resources.
     */
    @Test
    public void sendStatusTest() {
        Map<String, String> jobInfo = new HashMap<String, String>() {{
            put("a", "b");
        }};
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/post")) {
            HttpResponse response = client.sendStatus(new JobStatusPayload(Result.SUCCESS.toExportedObject(), "5", jobInfo, null));
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            try (InputStream content = response.getEntity().getContent()) {
                JsonNode requestTree = createMapper().readTree(content);
                Assert.assertNotNull(requestTree);

                // Check content type
                JsonNode headers = getAndAssertChild(requestTree, "headers", null);
                JsonNode contentType = getAndAssertChild(headers, "Content-Type", null);
                assertTrue(StringUtils.startsWith(contentType.asText(), "application/json"));

                // Check response body
                JsonNode child = getAndAssertChild(requestTree, "json", null);
                getAndAssertChild(child, "action", "status");
                getAndAssertChild(child, "status", Result.SUCCESS.toString());
                getAndAssertChild(child, "stepId", "5");
                JsonNode jobInfoNode = getAndAssertChild(child, "jobInfo", null);
                getAndAssertChild(jobInfoNode, "a", "b");

                // Check that there are no output resources
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
    public void sendStatusOutputResourcesTest() {
        String outputResources = "[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"},{\"content\":{\"c\":\"d\"},\"name\":\"resource2\"}]";
        try (JFrogPipelinesHttpClient client = createClient("http://httpbin.org/post")) {
            HttpResponse response = client.sendStatus(new JobStatusPayload(Result.SUCCESS.toExportedObject(), "5", new HashMap<>(), OutputResource.fromString(outputResources)));
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            try (InputStream content = response.getEntity().getContent()) {
                JsonNode requestTree = createMapper().readTree(content);
                Assert.assertNotNull(requestTree);

                JsonNode child = getAndAssertChild(requestTree, "json", null);
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

    private JFrogPipelinesHttpClient createClient(String integrationUrl) {
        return new JFrogPipelinesHttpClient(integrationUrl, "");
    }
}
