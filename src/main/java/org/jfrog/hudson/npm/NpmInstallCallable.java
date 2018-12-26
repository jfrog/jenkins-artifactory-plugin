package org.jfrog.hudson.npm;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmInstall;
import org.jfrog.hudson.pipeline.Utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmInstallCallable extends MasterToSlaveFileCallable<Build> {

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private String resolutionRepository; // Artifactory repository to download dependencies.
    private String executablePath; // npm executable path. Can be empty.
    private String args; // npm args.
    private String path; // Path to package.json or path to the directory that contains package.json.
    private Log logger;

    public NpmInstallCallable(ArtifactoryDependenciesClientBuilder dependenciesClientBuilder, String resolutionRepository, String executablePath, String args, String path, Log logger) {
        this.dependenciesClientBuilder = dependenciesClientBuilder;
        this.resolutionRepository = resolutionRepository;
        this.executablePath = executablePath;
        this.args = Objects.toString(args, "");
        this.path = path;
        this.logger = logger;
    }

    @Override
    public Build invoke(File file, VirtualChannel channel) {
        Path basePath = file.toPath();
        Path packagePath = StringUtils.isBlank(path) ? basePath : basePath.resolve(Utils.replaceTildeWithUserHome(path));
        return new NpmInstall(dependenciesClientBuilder, resolutionRepository, args, executablePath, logger, packagePath).execute();
    }
}
