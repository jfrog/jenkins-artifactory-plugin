package org.jfrog.hudson.pipeline.declarative.utils;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.build.api.util.Log;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.getBuildDataFileName;

/**
 * Create pipeline build data in @tmp/artifactory-pipeline-cache/build-number directory.
 * Used to transfer data between different steps in declarative pipelines.
 */
public class CreateBuildDataFileCallable extends MasterToSlaveFileCallable<Void> {

    private BuildDataFile buildDataFile;
    private String buildNumber;
    private Log logger;

    CreateBuildDataFileCallable(String buildNumber, BuildDataFile buildDataFile, Log logger) {
        this.buildNumber = buildNumber;
        this.buildDataFile = buildDataFile;
        this.logger = logger;
    }

    @Override
    public Void invoke(File tmpDir, VirtualChannel virtualChannel) throws IOException {
        Path artifactoryPipelineCacheDir = tmpDir.toPath().resolve(DeclarativePipelineUtils.PIPELINE_CACHE_DIR_NAME);
        DeclarativePipelineUtils.deleteOldBuildDataDirs(artifactoryPipelineCacheDir.toFile(), logger);
        Path buildDataDirPath = Files.createDirectories(artifactoryPipelineCacheDir.resolve(buildNumber));
        File buildDataFile = buildDataDirPath.resolve(getBuildDataFileName(this.buildDataFile.getStepName(), this.buildDataFile.getId())).toFile();
        if (buildDataFile.createNewFile()) {
            logger.debug(buildDataFile.getAbsolutePath() + " created");
            buildDataFile.deleteOnExit();
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(buildDataFile))
        ) {
            oos.writeObject(this.buildDataFile);
        }
        return null;
    }
}
