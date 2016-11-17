package org.jfrog.hudson.pipeline.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.generic.DependenciesDownloaderImpl;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.util.List;

/**
 * Created by romang on 4/19/16.
 */
public class GenericDownloadExecutor {
    private final Run build;
    private transient FilePath ws;
    private BuildInfo buildInfo;
    private ArtifactoryServer server;
    private Log log;
    private TaskListener listener;

    public GenericDownloadExecutor(ArtifactoryServer server, TaskListener listener, Run build, FilePath ws, BuildInfo buildInfo) {
        this.build = build;
        this.server = server;
        this.listener = listener;
        this.log = new JenkinsBuildInfoLog(listener);
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        this.ws = ws;
    }

    public BuildInfo execution(String spec) throws IOException, InterruptedException {
        CredentialsConfig preferredResolver = server.getDeployerCredentialsConfig();
        ArtifactoryDependenciesClient dependenciesClient = server.createArtifactoryDependenciesClient(
                preferredResolver.provideUsername(build.getParent()), preferredResolver.providePassword(build.getParent()),
                getProxyConfiguration(), listener);
        DependenciesDownloaderImpl dependenciesDownloader = new DependenciesDownloaderImpl(dependenciesClient, ws, log);
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(dependenciesDownloader, log);
        Spec downloadSpec = new SpecsHelper(log).getDownloadUploadSpec(spec);
        List<Dependency> resolvedDependencies = helper.downloadDependencies(server.getUrl(), downloadSpec);
        new BuildInfoAccessor(this.buildInfo).appendPublishedDependencies(resolvedDependencies);
        return this.buildInfo;
    }

    private ProxyConfiguration getProxyConfiguration() {
        hudson.ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        ProxyConfiguration proxyConfiguration = null;
        if (proxy != null) {
            proxyConfiguration = new ProxyConfiguration();
            proxyConfiguration.host = proxy.name;
            proxyConfiguration.port = proxy.port;
            proxyConfiguration.username = proxy.getUserName();
            proxyConfiguration.password = proxy.getPassword();
        }
        return proxyConfiguration;
    }
}
