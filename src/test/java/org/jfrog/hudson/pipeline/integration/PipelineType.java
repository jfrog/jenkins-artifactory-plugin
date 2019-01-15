package org.jfrog.hudson.pipeline.integration;

enum PipelineType {
    SCRIPTED,
    DECLARATIVE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
