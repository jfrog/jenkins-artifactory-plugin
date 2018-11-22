package org.jfrog.hudson.npm;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.extractor.NpmInstall;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.types.resolvers.NpmResolver;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.util.Objects;

public class NpmInstallCallable extends MasterToSlaveFileCallable<Module> {

    private transient Run build;
    private String executablePath;
    private NpmResolver resolver;
    private String installArgs;
    private Log logger;

    public NpmInstallCallable(Run build, String executablePath, NpmResolver resolver, String installArgs, TaskListener listener) {
        this.build = build;
        this.executablePath = executablePath;
        this.resolver = resolver;
        this.installArgs = Objects.toString(installArgs, "");
        this.logger = new JenkinsBuildInfoLog(listener);
    }

    @Override
    public Module invoke(File file, VirtualChannel channel) {
        ArtifactoryServer server = resolver.getArtifactoryServer();
        CredentialsConfig preferredResolver = server.getResolverCredentialsConfig();
        String serverUrl = server.getUrl();
        String username = preferredResolver.provideUsername(build.getParent());
        String password = preferredResolver.providePassword(build.getParent());
        try (ArtifactoryDependenciesClient dependenciesClient = new ArtifactoryDependenciesClient(serverUrl, username, password, logger)) {
            ProxyConfiguration proxyConfig = ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy);
            if (proxyConfig != null) {
                dependenciesClient.setProxyConfiguration(proxyConfig);
            }
            NpmInstall npmInstall = new NpmInstall(dependenciesClient, resolver.getRepo(), installArgs, executablePath, logger, file);
            return npmInstall.execute();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e), e);
        }
        return null;
    }
}
