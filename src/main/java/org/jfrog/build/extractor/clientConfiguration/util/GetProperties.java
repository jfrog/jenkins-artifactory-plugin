package org.jfrog.build.extractor.clientConfiguration.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.jfrog.build.extractor.UrlUtils.*;

public class GetProperties extends JFrogService<ArrayListMultimap<String, String>> {
    public static final String GET_PROPERTIES_ENDPOINT = "api/storage/";
    private final String relativePath;
    private final List<String> propertyKeys;

    public GetProperties(String relativePath, List<String> propertyKeys, Log log) {
        super(log);
        this.relativePath = relativePath;
        this.propertyKeys = propertyKeys;
    }

    public GetProperties(String relativePath, Log log) {
        this(relativePath, null, log);
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestBase = GET_PROPERTIES_ENDPOINT + encodeUrl(stripEnd(relativePath, "/")) + "?properties=";
        String requestUrl = requestBase;
        if (propertyKeys != null && !propertyKeys.isEmpty()) {
            ListIterator<String> properties = propertyKeys.listIterator();
            String s = properties.next();
            StringBuilder sb = new StringBuilder("?properties="+s);
            while (properties.hasNext()) {
                s = properties.next();
                sb.append(",");
                sb.append(s);
            }
            requestUrl = requestBase + sb.toString();
        }
        return new HttpGet(requestUrl);
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        ArrayListMultimap<String, String> propsMap = ArrayListMultimap.create();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(stream);
        JsonNode properties = jsonNode.get("properties");
        for (Iterator<String> it = properties.fieldNames(); it.hasNext(); ) {
            String property = it.next();
            JsonNode propList = properties.get(property);
            if (!propList.isArray()) {
                throw new RuntimeException("Api property " + property + " is not an array");
            }
            for (JsonNode keyNode : propList) {
                propsMap.put(property, keyNode.asText());
            }
        }
        result = propsMap;
    }
}
