package org.jfrog.hudson.go;

import com.google.common.collect.ArrayListMultimap;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.go.extractor.GoPublish;
import org.jfrog.hudson.pipeline.common.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GoPublishCallable extends MasterToSlaveFileCallable<Build> {

    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private ArrayListMultimap<String, String> properties;
    private String deploymentRepository;
    private String path;
    private String version;
    private String module;
    private Log logger;

    /**
     * @param artifactoryManagerBuilder - Build Info client builder.
     * @param properties                - Properties to set in the published artifact.
     * @param deploymentRepository      - Artifactory repository to deploy the artifacts.
     * @param path                      - Path to directory that contains go.mod.
     * @param logger                    - The logger.
     */
    public GoPublishCallable(ArtifactoryManagerBuilder artifactoryManagerBuilder, ArrayListMultimap<String, String> properties, String deploymentRepository, String path, String version, String module, Log logger) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.properties = properties;
        this.deploymentRepository = deploymentRepository;
        this.path = path;
        this.version = version;
        this.module = module;
        this.logger = logger;
    }

    @Override
    public Build invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
        Path basePath = file.toPath();
        Path packagePath = StringUtils.isBlank(path) ? basePath : basePath.resolve(Utils.replaceTildeWithUserHome(path));
        return new GoPublish(artifactoryManagerBuilder, properties, deploymentRepository, packagePath, version, module, logger).execute();
    }
}