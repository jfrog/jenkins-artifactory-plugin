package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateBuildFileCallable extends MasterToSlaveFileCallable<String> {
    private String buildNumber;
    private String stepName;
    private String stepId;
    private String content;

    public CreateBuildFileCallable(String buildNumber, String stepName, String stepId, String content) {
        this.buildNumber = buildNumber;
        this.stepName = stepName;
        this.stepId = stepId;
        this.content = content;
    }

    @Override
    public String invoke(File file, VirtualChannel virtualChannel) throws IOException {
        Path buildDir = Files.createDirectories(file.toPath().resolve(buildNumber));
        File buildFile = Files.createFile(buildDir.resolve(stepName + "_" + stepId)).toFile();
        try (PrintWriter out = new PrintWriter(buildFile)) {
            out.println(content);
        }
        buildFile.deleteOnExit();
        return buildFile.getAbsolutePath();
    }
}
