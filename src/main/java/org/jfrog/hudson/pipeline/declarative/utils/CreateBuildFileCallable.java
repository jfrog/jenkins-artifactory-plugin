package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.hudson.pipeline.declarative.types.BuildFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getBuildDataFileName;

public class CreateBuildFileCallable extends MasterToSlaveFileCallable<Void> {
    private String buildNumber;
    private BuildFile buildFile;

    CreateBuildFileCallable(String buildNumber, BuildFile buildFile) {
        this.buildNumber = buildNumber;
        this.buildFile = buildFile;
    }

    @Override
    public Void invoke(File file, VirtualChannel virtualChannel) throws IOException {
        Path buildDir = Files.createDirectories(file.toPath().resolve(buildNumber));
        file = Files.createFile(buildDir.resolve(getBuildDataFileName(buildFile.getStepName(), buildFile.getId()))).toFile();
        try (PrintWriter out = new PrintWriter(file)) {
            out.println(buildFile.getJsonObject());
        }
        file.deleteOnExit();
        return null;
    }
}
