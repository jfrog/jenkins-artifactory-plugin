package org.jfrog.hudson.pipeline.common.executors;

import antlr.ANTLRException;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.build.api.util.Log;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.trigger.ArtifactoryTrigger;
import org.jfrog.hudson.trigger.ArtifactoryTriggerInfo;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;

/**
 * @author yahavi
 */
public class BuildTriggerExecutor implements Executor {

    private final ArtifactoryServer server;
    private final Run<?, ?> build;
    private final String paths;
    private final String spec;
    private final Log logger;

    public BuildTriggerExecutor(Run<?, ?> build, TaskListener listener,
                                org.jfrog.hudson.pipeline.common.types.ArtifactoryServer server, String paths, String spec) {
        this.server = Utils.prepareArtifactoryServer(null, server);
        this.server.setServerId(server.getServerName());
        this.logger = new JenkinsBuildInfoLog(listener);
        this.build = build;
        this.paths = paths;
        this.spec = spec;
    }

    @Override
    public void execute() throws IOException {
        WorkflowJob job = (WorkflowJob) build.getParent();
        try {
            logger.debug("Setting trigger for '" + server.getArtifactoryUrl() + "'. Paths: '" + paths + "' spec: '" + spec + "'");
            ArtifactoryTrigger artifactoryTrigger = new ArtifactoryTrigger(paths, spec);
            ServerDetails details = new ServerDetails(server.getServerId(), server.getArtifactoryUrl(), null, null, null, null);
            artifactoryTrigger.setDetails(details);
            if (details.getArtifactoryName() == null) {
                // Save the Artifactory Server object in ArtifactoryTriggerInfo to use it during trigger runtime
                build.getParent().addOrReplaceAction(new ArtifactoryTriggerInfo(server));
            }
            job.addTrigger(artifactoryTrigger);
        } catch (ANTLRException e) {
            throw new IOException(e);
        }
    }
}
