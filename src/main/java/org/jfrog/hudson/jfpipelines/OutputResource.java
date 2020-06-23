package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.util.SerializationUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A key-value mapping of properties to send to JFrog Pipelines.
 */
@SuppressWarnings("unused")
public class OutputResource implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, String> content;
    private String name;

    // Default constructor for serialization
    public OutputResource() {
    }

    public OutputResource(String name, Map<String, String> content) {
        this.name = name;
        this.content = content;
    }

    public Map<String, String> getContent() {
        return content;
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public static List<OutputResource> fromString(@Nullable String str) throws JsonProcessingException {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        ObjectMapper mapper = SerializationUtils.createMapper();
        CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, OutputResource.class);
        return SerializationUtils.createMapper().readValue(str, javaType);
    }

}
