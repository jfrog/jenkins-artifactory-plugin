package org.jfrog.hudson.pipeline.common.types.buildInfo;

import com.google.common.collect.Maps;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.Issues;
import org.jfrog.build.api.IssuesCollectionConfig;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.issuesCollection.IssuesCollector;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import static org.jfrog.build.api.IssuesCollectionConfig.ISSUES_COLLECTION_ERROR_PREFIX;
import static org.jfrog.hudson.pipeline.common.Utils.prepareArtifactoryServer;


@SuppressWarnings("unused")
public class TrackedIssues implements Serializable {
    private transient CpsScript cpsScript;
    private Issues issues = new Issues();
    private String buildName;

    public TrackedIssues() {
    }

    // In declarative, build name is expected to be passed as an argument.
    public void collectBuildIssues(Run build, TaskListener listener, FilePath ws, String buildName, String config) throws IOException, InterruptedException {
        IssuesCollector collector = new IssuesCollector();
        IssuesCollectionConfig parsedConfig = collector.parseConfig(config);

        ArtifactoryBuildInfoClient client = getBuildInfoClient(parsedConfig.getIssues().getServerID(), build, listener);
        String previousRevision = collector.getPreviousVcsRevision(client, buildName);

        FilePath dotGitPath = Utils.getDotGitPath(ws);
        if (dotGitPath == null) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + "Could not find .git");
        }
        Issues oldIssues = issues;
        issues = dotGitPath.act(new MasterToSlaveFileCallable<Issues>() {
            public Issues invoke(File f, VirtualChannel channel) throws InterruptedException, IOException {
                return collector.doCollect(f, new JenkinsBuildInfoLog(listener), parsedConfig, previousRevision);
            }
        });

        issues.append(oldIssues);
    }

    private ArtifactoryBuildInfoClient getBuildInfoClient(String serverID, Run build, TaskListener listener) throws IOException {
        ArtifactoryServer server = prepareArtifactoryServer(serverID, null);
        if (server == null) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + "ServerID '" + serverID + "' does not exist");
        }
        return new BuildInfoAccessor(null).createArtifactoryClient(server, build, listener);
    }

    // Only interested in appending the issues themselves
    protected void append(TrackedIssues trackedIssuesToAppend) {
        if (trackedIssuesToAppend == null) {
            return;
        }
        if (this.issues == null) {
            this.issues = trackedIssuesToAppend.issues;
            return;
        }
        this.issues.append(trackedIssuesToAppend.issues);
    }

    @Whitelisted
    public void collect(String config) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("trackedIssues", this);
        stepVariables.put("config", config);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("collectIssues", stepVariables);
    }

    @Whitelisted
    public Issues getIssues() {
        return issues;
    }

    public void setIssues(Issues issues) {
        this.issues = issues;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }
}
