package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.model.Result;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.hudson.jfpipelines.payloads.JobStatusPayload;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.jfrog.hudson.util.SerializationUtils.createMapper;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobStatusPayloadTest {

    /**
     * Check that the payload is stringified without escaping.
     */
    @Test
    public void stringifyPayloadSingleOutputTest() {
        try {
            List<OutputResource> outputResources = OutputResource.fromString("[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"}]");
            JobStatusPayload payload = new JobStatusPayload(Result.SUCCESS.toExportedObject(), "5", outputResources);
            String text = createMapper().writeValueAsString(payload);
            assertTrue(text.contains("\"outputResources\":[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"}]"));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains("\"stepId\":\"5\""));
            assertTrue(text.contains("\"action\":\"status\""));
        } catch (JsonProcessingException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check that the payload is stringified properly with spaces.
     */
    @Test
    public void stringifyPayloadSpacesTest() {
        try {
            List<OutputResource> outputResources = OutputResource.fromString(" \n[\n{\"content\": {\"a\" : \"b\"} , \n\"name\" : \n\"resource1\"} ] \n");
            JobStatusPayload payload = new JobStatusPayload(Result.SUCCESS.toExportedObject(), "5", outputResources);
            String text = createMapper().writeValueAsString(payload);
            assertTrue(text.contains("\"outputResources\":[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"}]"));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains("\"stepId\":\"5\""));
            assertTrue(text.contains("\"action\":\"status\""));
        } catch (JsonProcessingException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check that the payload is stringified without escaping.
     */
    @Test
    public void stringifyPayloadTwoOutputsTest() {
        try {
            List<OutputResource> outputResources = OutputResource.fromString("[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"},{\"content\":{\"c\":\"d\"},\"name\":\"resource2\"}]");
            JobStatusPayload payload = new JobStatusPayload(Result.SUCCESS.toExportedObject(), "5", outputResources);
            String text = createMapper().writeValueAsString(payload);
            assertTrue(text.contains("\"outputResources\":[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"},{\"content\":{\"c\":\"d\"},\"name\":\"resource2\"}]"));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains("\"stepId\":\"5\""));
            assertTrue(text.contains("\"action\":\"status\""));
        } catch (JsonProcessingException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check that the payload is stringified without null payload.
     */
    @Test
    public void stringifyPayloadNullResourcesTest() {
        try {
            JobStatusPayload payload = new JobStatusPayload(Result.SUCCESS.toExportedObject(), "5", null);
            String text = createMapper().writeValueAsString(payload);
            assertFalse(text.contains("outputResources"));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains("\"stepId\":\"5\""));
            assertTrue(text.contains("\"action\":\"status\""));
        } catch (JsonProcessingException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
