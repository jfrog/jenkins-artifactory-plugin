package org.jfrog.hudson.npm;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmInstall;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmInstallCallable extends MasterToSlaveFileCallable<Module> {

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private String resolutionRepository;
    private String executablePath;
    private String args;
    private String path;
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
    public Module invoke(File file, VirtualChannel channel) {
        Path basePath = file.toPath();
        Path packagePath = StringUtils.isBlank(path) ? basePath : basePath.resolve(path.replaceFirst("^~", System.getProperty("user.home")));
        return new NpmInstall(dependenciesClientBuilder, resolutionRepository, args, executablePath, logger, packagePath).execute();
    }
}
