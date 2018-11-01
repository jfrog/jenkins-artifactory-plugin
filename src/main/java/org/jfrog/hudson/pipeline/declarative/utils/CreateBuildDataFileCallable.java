package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getBuildDataFileName;

/**
 * Create pipeline build data in @tmp directory.
 * Used to pass data from different steps in declarative pipelines.
 */
public class CreateBuildDataFileCallable extends MasterToSlaveFileCallable<Void> {
    private String buildNumber;
    private BuildDataFile buildDataFile;

    CreateBuildDataFileCallable(String buildNumber, BuildDataFile buildDataFile) {
        this.buildNumber = buildNumber;
        this.buildDataFile = buildDataFile;
    }

    @Override
    public Void invoke(File file, VirtualChannel virtualChannel) throws IOException {
        Path buildDir = Files.createDirectories(file.toPath().resolve(buildNumber));
        file = buildDir.resolve(getBuildDataFileName(buildDataFile.getStepName(), buildDataFile.getId())).toFile();
        if (file.createNewFile()) {
            file.deleteOnExit();
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            oos.writeObject(buildDataFile);
        }
        return null;
    }
}
