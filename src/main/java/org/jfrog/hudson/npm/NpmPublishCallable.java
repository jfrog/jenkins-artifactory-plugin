package org.jfrog.hudson.npm;

import com.google.common.collect.ArrayListMultimap;
import hudson.EnvVars;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmPublish;
import org.jfrog.hudson.pipeline.common.Utils;

import java.io.File;
import java.nio.file.Path;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmPublishCallable extends MasterToSlaveFileCallable<Build> {

    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private ArrayListMultimap<String, String> properties;
    private String deploymentRepository;
    private String executablePath;
    private String path;
    private EnvVars env;
    private Log logger;

    /**
     * @param buildInfoClientBuilder - Build Info client builder.
     * @param properties             - Properties to set in the published artifact.
     * @param deploymentRepository   - Artifactory repository to deploy the artifacts.
     * @param executablePath         - npm executable path. Can be empty.
     * @param path                   - Path to package.json or path to the directory that contains package.json.
     * @param env                    - Environment variables to use during npm execution.
     * @param logger                 - The logger.
     */
    public NpmPublishCallable(ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder, ArrayListMultimap<String, String> properties, String deploymentRepository, String executablePath, String path, EnvVars env, Log logger) {
        this.buildInfoClientBuilder = buildInfoClientBuilder;
        this.properties = properties;
        this.deploymentRepository = deploymentRepository;
        this.executablePath = executablePath;
        this.path = path;
        this.env = env;
        this.logger = logger;
    }

    @Override
    public Build invoke(File file, VirtualChannel channel) {
        Path basePath = file.toPath();
        Path packagePath = StringUtils.isBlank(path) ? basePath : basePath.resolve(Utils.replaceTildeWithUserHome(path));
        return new NpmPublish(buildInfoClientBuilder, properties, executablePath, packagePath, deploymentRepository, logger, env).execute();
    }
}