package org.jfrog.hudson.go;

import hudson.EnvVars;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.go.extractor.GoRun;
import org.jfrog.hudson.pipeline.common.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GoRunCallable extends MasterToSlaveFileCallable<Build> {

    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private String resolutionRepository;
    private String path;
    private String goCmdArgs;
    private String module;
    private String resolverUsername;
    private String resolverPassword;
    private Log logger;
    private EnvVars env;

    /**
     * @param goCmdArgs - Artifactory repository to deploy the artifacts.
     * @param path      - Path to directory that contains go.mod.
     * @param logger    - The logger.
     */
    public GoRunCallable(String path, String goCmdArgs, String module, Log logger, EnvVars env) {
        this.path = path;
        this.goCmdArgs = goCmdArgs;
        this.module = module;
        this.logger = logger;
        this.env = env;
        this.buildInfoClientBuilder = null;
    }

    public void setResolverDetails(ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder, String resolutionRepository, String username, String password) {
        this.buildInfoClientBuilder = buildInfoClientBuilder;
        this.resolutionRepository = resolutionRepository;
        this.resolverUsername = username;
        this.resolverPassword = password;
    }

    @Override
    public Build invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
        Path basePath = file.toPath();
        Path packagePath = StringUtils.isBlank(path) ? basePath : basePath.resolve(Utils.replaceTildeWithUserHome(path));
        return new GoRun(goCmdArgs, packagePath, module, buildInfoClientBuilder, resolutionRepository, resolverUsername, resolverPassword, logger, env).execute();
    }
}