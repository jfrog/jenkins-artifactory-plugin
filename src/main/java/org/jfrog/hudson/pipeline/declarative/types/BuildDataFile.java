package org.jfrog.hudson.pipeline.declarative.types;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jfrog.hudson.pipeline.Utils;

import java.io.Serializable;

/**
 * Data to transfer between different declarative pipeline steps.
 * Contains stepName, stepId and step parameters.
 */
public class BuildDataFile implements Serializable {

    private static final long serialVersionUID = 1L;
    private ObjectNode jsonObject;

    public BuildDataFile(String stepName, String stepId) {
        jsonObject = Utils.mapper().createObjectNode();
        jsonObject.put("stepName", stepName).put("stepId", stepId);
    }

    public BuildDataFile put(String key, String value) {
        jsonObject.put(key, value);
        return this;
    }

    public String getStepName() {
        return jsonObject.get("stepName").asText();
    }

    public String getId() {
        return jsonObject.get("stepId").asText();
    }

    @Override
    public String toString() {
        return jsonObject.toString();
    }
}
