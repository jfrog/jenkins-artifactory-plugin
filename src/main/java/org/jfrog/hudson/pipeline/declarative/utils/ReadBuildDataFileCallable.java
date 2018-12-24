package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getBuildDataFileName;

/**
 *  Read pipeline build data from @tmp/build-number directory.
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
    public BuildDataFile invoke(File file, VirtualChannel virtualChannel) throws IOException {
        Path buildDir = file.toPath().resolve(buildNumber);
        File buildFile = buildDir.resolve(getBuildDataFileName(stepName, stepId)).toFile();
        if (!buildFile.exists()) {
            return null;
        }
        try (FileInputStream fos = new FileInputStream(buildFile);
             ObjectInputStream oos = new ObjectInputStream(fos)
        ) {
            return (BuildDataFile) oos.readObject();
        } catch (ClassNotFoundException e) {
            listener.error(e.getMessage());
            return null;
        }
    }
}
