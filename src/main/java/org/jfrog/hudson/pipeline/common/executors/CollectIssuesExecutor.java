package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.issuesCollection.IssuesCollector;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Issues;
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
        // Get all necessary arguments for the command
        IssuesCollector collector = new IssuesCollector();
        ArtifactoryBuildInfoClient client = getBuildInfoClient(pipelineServer, build, listener);

        // Collect issues
        org.jfrog.build.api.Issues newIssues = ws.act(new MasterToSlaveFileCallable<org.jfrog.build.api.Issues>() {
            public org.jfrog.build.api.Issues invoke(File f, VirtualChannel channel) throws InterruptedException, IOException {
                return collector.collectIssues(f, new JenkinsBuildInfoLog(listener), config, client, buildName);
            }
        });

        // Convert and append Issues
        this.issues.convertAndAppend(newIssues);
    }

    private ArtifactoryBuildInfoClient getBuildInfoClient(ArtifactoryServer pipelineServer, Run build, TaskListener listener) {
        org.jfrog.hudson.ArtifactoryServer server = Utils.prepareArtifactoryServer(null, pipelineServer);
        return new BuildInfoAccessor(null).createArtifactoryClient(server, build, listener);
    }
}
