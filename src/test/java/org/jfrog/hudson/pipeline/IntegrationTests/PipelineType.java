package org.jfrog.hudson.pipeline.IntegrationTests;

enum PipelineType {
    SCRIPTED,
    DECLARATIVE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
