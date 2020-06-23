package org.jfrog.hudson.jfpipelines;

import java.io.Serializable;

/**
 * Used for serializing information from 'jfPipelines' step
 */
public class JFrogPipelinesJobInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String outputResources;
    private boolean reported;

    /**
     * Empty constructor for serialization
     */
    @SuppressWarnings("unused")
    public JFrogPipelinesJobInfo() {
    }

    public void setOutputResources(String outputResources) {
        this.outputResources = outputResources;
    }

    public String getOutputResources() {
        return outputResources;
    }

    public void setReported() {
        this.reported = true;
    }

    public boolean isReported() {
        return reported;
    }
}
