package org.jfrog.hudson.jfpipelines;

import java.util.Map;

public class OutputResource {
    private final String name;
    private final Map<String, String> content;

    public OutputResource(String name, Map<String, String> content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getContent() {
        return content;
    }
}
