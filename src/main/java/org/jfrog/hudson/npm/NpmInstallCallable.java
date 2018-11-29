package org.jfrog.hudson.npm;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.npm.extractor.NpmInstall;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.types.resolvers.NpmResolver;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.util.Objects;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmInstallCallable extends MasterToSlaveFileCallable<Module> {

    private transient Run build;
    private TaskListener listener;
    private String executablePath;
    private NpmResolver resolver;
    private String args;

    public NpmInstallCallable(Run build, TaskListener listener, String executablePath, NpmResolver resolver, String args) {
        this.build = build;
        this.listener = listener;
        this.executablePath = executablePath;
        this.resolver = resolver;
        this.args = Objects.toString(args, "");
    }

    @Override
    public Module invoke(File file, VirtualChannel channel) {
        Log logger = new JenkinsBuildInfoLog(listener);
        ArtifactoryServer server = resolver.getArtifactoryServer();
        CredentialsConfig preferredResolver = server.getResolverCredentialsConfig();
        ArtifactoryDependenciesClientBuilder clientBuilder = new ArtifactoryDependenciesClientBuilder()
                .setArtifactoryUrl(server.getUrl())
                .setUsername(preferredResolver.provideUsername(build.getParent()))
                .setPassword(preferredResolver.providePassword(build.getParent()))
                .setProxyConfiguration(ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy))
                .setLog(logger)
                .setConnectionRetry(server.getConnectionRetry())
                .setConnectionTimeout(server.getTimeout());
        try {
            return new NpmInstall(clientBuilder, resolver.getRepo(), args, executablePath, logger, file.toPath()).execute();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e), e);
        }
        return null;
    }
}
