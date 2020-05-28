package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Result;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JobCompletedPayloadTest {

    /**
     * Check that the payload is stringified without escaping.
     */
    @Test
    public void stringifyPayloadTest() {
        String outputResources = "[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"},{\"content\":{\"c\":\"d\"},\"name\":\"resource2\"}]";
        JobCompletedPayload payload = new JobCompletedPayload(Result.SUCCESS, "5", outputResources);
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        try {
            String text = mapper.writeValueAsString(payload);
            assertTrue(text.contains("\"outputResources\":[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"},{\"content\":{\"c\":\"d\"},\"name\":\"resource2\"}]"));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains("\"stepId\":\"5\""));
            assertTrue(text.contains("\"action\":\"status\""));
        } catch (JsonProcessingException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Test
    public void stringifyPayloadNullResourcesTest() {
        JobCompletedPayload payload = new JobCompletedPayload(Result.SUCCESS, "5", null);
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        try {
            String text = mapper.writeValueAsString(payload);
            assertFalse(text.contains("outputResources"));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains("\"stepId\":\"5\""));
            assertTrue(text.contains("\"action\":\"status\""));
        } catch (JsonProcessingException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Test
    public void stringifyPayloadEmptyResourcesTest() {
        JobCompletedPayload payload = new JobCompletedPayload(Result.SUCCESS, "5", "");
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        try {
            String text = mapper.writeValueAsString(payload);
            assertFalse(text.contains("outputResources"));
            assertTrue(text.contains("\"status\":\"SUCCESS\""));
            assertTrue(text.contains("\"stepId\":\"5\""));
            assertTrue(text.contains("\"action\":\"status\""));
        } catch (JsonProcessingException e) {
            Assert.fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
