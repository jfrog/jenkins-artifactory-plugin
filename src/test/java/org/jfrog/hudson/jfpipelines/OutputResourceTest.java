package org.jfrog.hudson.jfpipelines;

import com.google.common.collect.ImmutableMap;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class OutputResourceTest {

    private static final String JSON_KEY = "outputResources";

    @Test
    public void noValuesTest() {
        OutputResource[] outputResources = new OutputResource[]{};
        JSONObject jsonObject = new JSONObject();
        jsonObject.element(JSON_KEY, outputResources);
        Assert.assertEquals("{\"" + JSON_KEY + "\":[]}", jsonObject.toString());
    }

    @Test
    public void singleTest() {
        Map<String, String> values = ImmutableMap.of("a", "b");
        OutputResource[] outputResource = {new OutputResource("singleTest", values)};
        JSONObject jsonObject = new JSONObject();
        jsonObject.element(JSON_KEY, outputResource);
        Assert.assertEquals("{\"" + JSON_KEY + "\":[{\"content\":{\"a\":\"b\"},\"name\":\"singleTest\"}]}", jsonObject.toString());
    }

    @Test
    public void twoValuesTest() {
        Map<String, String> values = ImmutableMap.of("a", "b", "c", "d");
        OutputResource[] outputResource = {new OutputResource("twoValuesTest", values)};
        JSONObject jsonObject = new JSONObject();
        jsonObject.element(JSON_KEY, outputResource);
        Assert.assertEquals("{\"" + JSON_KEY + "\":[{\"content\":{\"a\":\"b\",\"c\":\"d\"},\"name\":\"twoValuesTest\"}]}", jsonObject.toString());
    }

    @Test
    public void twoResourcesTest() {
        OutputResource[] outputResources = {
                new OutputResource("resource1", ImmutableMap.of("a", "b")),
                new OutputResource("resource2", ImmutableMap.of("c", "d"))
        };
        JSONObject jsonObject = new JSONObject();
        jsonObject.element(JSON_KEY, outputResources);
        Assert.assertEquals("{\"" + JSON_KEY + "\":[{\"content\":{\"a\":\"b\"},\"name\":\"resource1\"},{\"content\":{\"c\":\"d\"},\"name\":\"resource2\"}]}", jsonObject.toString());

    }
}
