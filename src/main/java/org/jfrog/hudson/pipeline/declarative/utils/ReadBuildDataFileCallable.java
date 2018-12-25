package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getBuildDataFileName;

/**
 *  Read pipeline build data from @tmp/artifactory-pipeline-cache/build-number directory.
 *  Used to transfer data between different steps in declarative pipelines.
 */
public class ReadBuildDataFileCallable extends MasterToSlaveFileCallable<BuildDataFile> {

    private TaskListener listener;
    private String buildNumber;
    private String stepName;
    private String stepId;

    ReadBuildDataFileCallable(TaskListener listener, String buildNumber, String stepName, String stepId) {
        this.listener = listener;
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
            listener.error(ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
