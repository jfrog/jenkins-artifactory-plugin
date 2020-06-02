package org.jfrog.hudson.pipeline.declarative;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;

import static org.jfrog.hudson.util.SerializationUtils.createMapper;

/**
 * Data to transfer between different declarative pipeline steps.
 * Contains stepName, stepId and step parameters.
 */
public class BuildDataFile implements Serializable {

    private static final long serialVersionUID = 1L;
    private ObjectNode jsonObject; // Root node

    /**
     * Data to transfer between different declarative pipeline steps.
     *
     * @param stepName - The step name. Can be 'rtServer', 'rtMavenRun', 'rtGradleRun', etc.
     * @param stepId   - Unique value from the user to distinguish between steps. We may have, for example, 2 build infos.
     */
    public BuildDataFile(String stepName, String stepId) {
        jsonObject = createMapper().createObjectNode();
        jsonObject.put("stepName", stepName).put("stepId", stepId);
    }

    public BuildDataFile put(String key, String value) {
        jsonObject.put(key, value);
        return this;
    }

    /**
     * Put serializable objects like MavenDeployer, MavenResolver, ArtifactoryServer, etc.
     *
     * @param pojo - Serializable object
     */
    public void putPOJO(Object pojo) {
        jsonObject.putPOJO(jsonObject.get("stepName").asText(), pojo);
    }

    public JsonNode get(String key) {
        return jsonObject.get(key);
    }

    public String getStepName() {
        return jsonObject.get("stepName").asText();
    }

    public String getId() {
        return jsonObject.get("stepId").asText();
    }

    /**
     * For serialization.
     *
     * @param stream - The input stream to read the object from.
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        jsonObject = createMapper().readValue((DataInput) stream, ObjectNode.class);
    }

    /**
     * For serialization.
     *
     * @param stream - The output stream to write the object to.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        createMapper().writeValue((DataOutput) stream, jsonObject);
    }
}
