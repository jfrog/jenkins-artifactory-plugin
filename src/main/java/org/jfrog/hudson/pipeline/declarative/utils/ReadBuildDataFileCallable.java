package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getBuildDataFileName;

/**
 * Read pipeline build data from @tmp/artifactory-pipeline-cache/build-number directory.
 * Used to transfer data between different steps in declarative pipelines.
 */
public class ReadBuildDataFileCallable extends MasterToSlaveFileCallable<BuildDataFile> {

    private String buildNumber;
    private String stepName;
    private String stepId;

    ReadBuildDataFileCallable(String buildNumber, String stepName, String stepId) {
        this.buildNumber = buildNumber;
        this.stepName = stepName;
        this.stepId = stepId;
    }

    @Override
    public BuildDataFile invoke(File tmpDir, VirtualChannel virtualChannel) throws IOException {
        Path artifactoryPipelineCacheDir = tmpDir.toPath().resolve(DeclarativePipelineUtils.PIPELINE_CACHE_DIR_NAME);
        Path buildDataDirPath = artifactoryPipelineCacheDir.resolve(buildNumber);
        File buildDataFile = buildDataDirPath.resolve(getBuildDataFileName(stepName, stepId)).toFile();
        if (!buildDataFile.exists()) {
            return null;
        }
        try (FileInputStream fos = new FileInputStream(buildDataFile);
             ObjectInputStream oos = new ObjectInputStream(fos)
        ) {
            return (BuildDataFile) oos.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
