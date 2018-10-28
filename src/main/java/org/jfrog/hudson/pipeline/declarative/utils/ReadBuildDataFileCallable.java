package org.jfrog.hudson.pipeline.declarative.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.hudson.pipeline.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getBuildDataFileName;

/**
 *  Read pipeline build data from @tmp directory.
 *  Used to pass data from different steps in declarative pipelines.
 */
public class ReadBuildDataFileCallable extends MasterToSlaveFileCallable<JsonNode> {
    private String buildNumber;
    private String stepName;
    private String stepId;

    ReadBuildDataFileCallable(String buildNumber, String stepName, String stepId) {
        this.buildNumber = buildNumber;
        this.stepName = stepName;
        this.stepId = stepId;
    }

    @Override
    public JsonNode invoke(File file, VirtualChannel virtualChannel) throws IOException {
        Path buildDir = file.toPath().resolve(buildNumber);
        File buildFile = buildDir.resolve(getBuildDataFileName(stepName, stepId)).toFile();
        if (!buildFile.exists()) {
            return NullNode.getInstance();
        }
        return Utils.mapper().readTree(buildFile);
    }
}
