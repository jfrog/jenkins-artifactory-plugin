package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.issuesCollection.IssuesCollector;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Issues;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.io.IOException;


public class CollectIssuesExecutor implements Executor {
    private transient Run build;
    private transient TaskListener listener;
    private transient FilePath ws;
    private String buildName;
    private String config;
    private Issues issues;
    private ArtifactoryServer pipelineServer;

    // In declarative, build name is expected to be passed as an argument (in scripted taken from the object).
    public CollectIssuesExecutor(Run build, TaskListener listener, FilePath ws, String buildName,
                                 String config, Issues issues, ArtifactoryServer pipelineServer) {
        this.build = build;
        this.listener = listener;
        this.ws = ws;
        this.buildName = buildName;
        this.config = config;
        this.issues = issues;
        this.pipelineServer = pipelineServer;
    }

    public void execute() throws IOException, InterruptedException {
        ArtifactoryBuildInfoClientBuilder clientBuilder = getBuildInfoClientBuilder(pipelineServer, build, listener);

        // Collect issues
        org.jfrog.build.api.Issues newIssues = ws.act(new CollectIssuesCallable(new JenkinsBuildInfoLog(listener), config, clientBuilder, buildName));

        // Convert and append Issues
        this.issues.convertAndAppend(newIssues);
    }

    private ArtifactoryBuildInfoClientBuilder getBuildInfoClientBuilder(ArtifactoryServer pipelineServer, Run build, TaskListener listener) {
        org.jfrog.hudson.ArtifactoryServer server = Utils.prepareArtifactoryServer(null, pipelineServer);
        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(new ArtifactoryConfigurator(server), server);
        return server.createBuildInfoClientBuilder(preferredDeployer.provideCredentials(build.getParent()),
                server.createProxyConfiguration(Jenkins.getInstance().proxy), new JenkinsBuildInfoLog(listener));
    }

    public static class CollectIssuesCallable extends MasterToSlaveFileCallable<org.jfrog.build.api.Issues> {
        private Log logger;
        private String config;
        private ArtifactoryBuildInfoClientBuilder clientBuilder;
        private String buildName;

        CollectIssuesCallable(Log logger, String config, ArtifactoryBuildInfoClientBuilder clientBuilder,
                              String buildName) {
            this.logger = logger;
            this.config = config;
            this.clientBuilder = clientBuilder;
            this.buildName = buildName;
        }

        public org.jfrog.build.api.Issues invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
            IssuesCollector issuesCollector = new IssuesCollector();
            return issuesCollector.collectIssues(file, logger, config, clientBuilder, buildName);
        }
    }
}
