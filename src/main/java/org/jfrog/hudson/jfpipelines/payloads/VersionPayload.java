package org.jfrog.hudson.jfpipelines.payloads;

import java.io.Serializable;

@SuppressWarnings("unused")
public class VersionPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ACTION = "test";

    public String getAction() {
        return ACTION;
    }
}
