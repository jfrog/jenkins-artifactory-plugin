package org.jfrog.hudson.npm;

import com.google.common.collect.ArrayListMultimap;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmPublish;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.types.deployers.NpmDeployer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.util.Objects;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmPublishCallable extends MasterToSlaveFileCallable<Module> {

    private transient Run build;
    private ArrayListMultimap<String, String> properties;
    private String executablePath;
    private NpmDeployer deployer;
    private String args;
    private Log logger;

    public NpmPublishCallable(Run build, ArrayListMultimap<String, String> properties, String executablePath, NpmDeployer deployer, String args, TaskListener listener) {
        this.build = build;
        this.properties = properties;
        this.executablePath = executablePath;
        this.deployer = deployer;
        this.args = Objects.toString(args, "");
        this.logger = new JenkinsBuildInfoLog(listener);
    }

    @Override
    public Module invoke(File file, VirtualChannel channel) {
        ArtifactoryServer server = deployer.getArtifactoryServer();
        CredentialsConfig preferredDeployer = server.getDeployerCredentialsConfig();
        ArtifactoryBuildInfoClientBuilder clientBuilder = new ArtifactoryBuildInfoClientBuilder()
                .setArtifactoryUrl(server.getUrl())
                .setUsername(preferredDeployer.provideUsername(build.getParent()))
                .setPassword(preferredDeployer.providePassword(build.getParent()))
                .setProxyConfiguration(ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy))
                .setLog(logger)
                .setConnectionRetry(server.getConnectionRetry())
                .setConnectionTimeout(server.getTimeout());
        try {
            return new NpmPublish(clientBuilder, properties, executablePath, file.toPath(), deployer.getRepo(), args).execute();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e), e);
        }
        return null;
    }
}