package org.jfrog.hudson.pipeline.integrationtests;

enum PipelineType {
    SCRIPTED,
    DECLARATIVE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
