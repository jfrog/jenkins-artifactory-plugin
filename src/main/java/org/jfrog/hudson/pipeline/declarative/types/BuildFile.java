package org.jfrog.hudson.pipeline.declarative.types;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jfrog.hudson.pipeline.Utils;

public class BuildFile {
    private ObjectNode jsonObject;

    public BuildFile(String stepName, String stepId) {
        jsonObject = Utils.mapper().createObjectNode();
        jsonObject.put("stepName", stepName).put("stepId", stepId);
    }

    public BuildFile put(String key, String value) {
        jsonObject.put(key, value);
        return this;
    }

    public String getStepName() {
        return jsonObject.get("stepName").asText();
    }

    public String getId() {
        return jsonObject.get("stepId").asText();
    }

    public ObjectNode getJsonObject() {
        return jsonObject;
    }
}
