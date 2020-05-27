package org.jfrog.hudson.jfpipelines;

import java.io.Serializable;
import java.util.Map;

public class OutputResource implements Serializable {
    private final String name;
    private final Map<String, String> content;

    public OutputResource(String name, Map<String, String> content) {
        this.name = name;
        this.content = content;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public Map<String, String> getContent() {
        return content;
    }
}
