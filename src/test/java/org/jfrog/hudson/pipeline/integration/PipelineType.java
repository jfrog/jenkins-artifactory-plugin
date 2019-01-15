package org.jfrog.hudson.pipeline.integration;

/**
 * @author yahavi
 */
enum PipelineType {
    SCRIPTED,
    DECLARATIVE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
