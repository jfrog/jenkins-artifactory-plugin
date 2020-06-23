package org.jfrog.hudson;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;

public class TestUtils {

    /**
     * Get JsonNode, assert and return its child.
     *
     * @param jsonNode - The node
     * @param name     - The name of the child
     * @param value    - The value to assert. Can be null if no value.
     * @return the child
     */
    public static JsonNode getAndAssertChild(JsonNode jsonNode, String name, String value) {
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
    public static JsonNode getAndAssertChild(JsonNode jsonNode, int index) {
        JsonNode child = jsonNode.get(index);
        Assert.assertNotNull(child);
        return child;
    }

}
