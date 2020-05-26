package org.jfrog.hudson.jfpipelines;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OutputResourceTest {

    private static final String KEY = "outputResources";

    @Test
    public void singleValueTest() {
        String name = "testName";
        Map<String, String> values = new HashMap<String, String>(){{
            put("a", "b");
        }};
        OutputResource outputResource = new OutputResource(name, values);
        JSONObject jsonObject = new JSONObject();
        jsonObject.element(KEY, outputResource);
        Assert.assertEquals("{\"outputResources\":{\"content\":{\"a\":\"b\"},\"name\":\"testName\"}}", jsonObject.toString());
    }

}
