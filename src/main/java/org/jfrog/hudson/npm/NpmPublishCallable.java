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
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
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
        String username = preferredDeployer.provideUsername(build.getParent());
        String password = preferredDeployer.providePassword(build.getParent());
        ProxyConfiguration proxyConfig = ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy);
        try (ArtifactoryBuildInfoClient buildInfoClient = server.createArtifactoryClient(username, password, proxyConfig, logger)) {
            return new NpmPublish(buildInfoClient, properties, executablePath, file.toPath(), deployer.getRepo(), args).execute();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e), e);
        }
        return null;
    }
}