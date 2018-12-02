package org.jfrog.hudson.npm;

import com.google.common.collect.ArrayListMultimap;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmPublish;
import org.jfrog.hudson.pipeline.types.deployers.NpmDeployer;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmPublishCallable extends MasterToSlaveFileCallable<Module> {

    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private ArrayListMultimap<String, String> properties;
    private String executablePath;
    private NpmDeployer deployer;
    private String args;
    private String path;
    private Log logger;

    public NpmPublishCallable(ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder, ArrayListMultimap<String, String> properties, String executablePath, NpmDeployer deployer, String args, String path, Log logger) {
        this.buildInfoClientBuilder = buildInfoClientBuilder;
        this.properties = properties;
        this.executablePath = executablePath;
        this.deployer = deployer;
        this.args = Objects.toString(args, "");
        this.path = path;
        this.logger = logger;
    }

    @Override
    public Module invoke(File file, VirtualChannel channel) {
        Path basePath = file.toPath();
        Path packagePath = StringUtils.isBlank(path) ? basePath : basePath.resolve(path.replaceFirst("^~", System.getProperty("user.home")));
        return new NpmPublish(buildInfoClientBuilder, properties, executablePath, packagePath, deployer.getRepo(), logger, args).execute();
    }
}