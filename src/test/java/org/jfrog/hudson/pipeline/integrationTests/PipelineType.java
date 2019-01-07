package org.jfrog.hudson.pipeline.integrationTests;

enum PipelineType {
    SCRIPTED,
    DECLARATIVE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
