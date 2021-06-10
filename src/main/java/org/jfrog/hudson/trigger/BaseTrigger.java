package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.model.BuildableItem;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.build.client.ItemLastModified;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;
import org.jfrog.hudson.util.RepositoriesUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author yahavi
 **/
public abstract class BaseTrigger<JobType extends BuildableItem> extends Trigger<JobType> {
    static final Logger logger = Logger.getLogger(JenkinsBuildInfoLog.class.getName());
    private long lastModified = System.currentTimeMillis();
    ServerDetails details;
    String branches;
    String paths;

    BaseTrigger(String spec, String paths, String branches) throws ANTLRException {
        super(spec);
        this.paths = StringUtils.trimToEmpty(paths);
        this.branches = StringUtils.trimToEmpty(branches);
    }

    public void setDetails(ServerDetails details) {
        this.details = details;
    }

    public ServerDetails getDetails() {
        return details;
    }

    @SuppressWarnings("unused")
    public String getBranches() {
        return this.branches;
    }

    public String getPaths() {
        return this.paths;
    }

    public void run() {
        if (job == null) {
            return;
        }
        ArtifactoryServer server = getArtifactoryServer();
        try (ArtifactoryManager artifactoryManager = server.createArtifactoryManager(server.getDeployerCredentialsConfig().provideCredentials(job), ProxyUtils.createProxyConfiguration())) {
            String[] paths = this.paths.split(";");
            for (String path : paths) {
                ItemLastModified itemLastModified = artifactoryManager.getItemLastModified(StringUtils.trimToEmpty(path));
                long responseLastModified = itemLastModified.getLastModified();
                if (responseLastModified > lastModified) {
                    this.lastModified = responseLastModified;
                    for (BuildableItem jobToTrigger : getJobsToTrigger()) {
                        logger.fine("Updating " + jobToTrigger.getName());
                        jobToTrigger.save();
                        jobToTrigger.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
                    }
                } else {
                    logger.fine(String.format("Artifactory Trigger did not trigger job %s, since last modified time: %d is earlier or equal than %d for path %s", job.getName(), responseLastModified, lastModified, path));
                }
            }
        } catch (IOException | ParseException e) {
            logger.severe("Artifactory Trigger encountered an unexpected error:" + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    public void stop() {
        if (job == null) {
            return;
        }
        logger.info("Stopping " + job.getName() + " Artifactory trigger.");
        super.stop();
    }

    /**
     * Get selected Artifactory server when trigger starts.
     */
    public ArtifactoryServer getArtifactoryServer() {
        if (details == null) {
            return null;
        }
        String serverId = details.getArtifactoryName();
        return StringUtils.isBlank(serverId) ? getArtifactoryServerFromPipeline() : getGlobalArtifactoryServer(serverId);
    }

    /**
     * Get Artifactory server configured in pipeline.
     *
     * @return Artifactory server or null
     */
    private ArtifactoryServer getArtifactoryServerFromPipeline() {
        if (!(job instanceof WorkflowJob)) {
            // Not a pipeline job
            return null;
        }
        ArtifactoryTriggerInfo info = ((WorkflowJob) job).getAction(ArtifactoryTriggerInfo.class);
        return info != null ? info.getServer() : null;
    }

    /**
     * Get Artifactory server from the JFrog Instances configuration by server ID. Throw RuntimeException if not exist.
     *
     * @param serverId - The server ID to look
     * @return artifactory server
     */
    private ArtifactoryServer getGlobalArtifactoryServer(String serverId) {
        ArtifactoryServer server = RepositoriesUtils.getArtifactoryServer(serverId);
        if (server == null) {
            handleServerNotExist(serverId);
        }
        return server;
    }

    /**
     * Log warning and throw RuntimeException if the Artifactory server does not exist.
     *
     * @param serverId - The server ID or null
     */
    private void handleServerNotExist(String serverId) {
        String message = "Artifactory Trigger failed triggering the job, since Artifactory server ";
        if (StringUtils.isNotBlank(serverId)) {
            message += "'" + serverId + "' ";
        }
        message += "does not exist.";
        logger.warning(message);
        throw new RuntimeException(message);
    }

    abstract List<BuildableItem> getJobsToTrigger();

    /**
     * Get the selected server id. Used in the Jelly to show the selected server.
     *
     * @return the server ID
     */
    @SuppressWarnings("unused")
    public String getSelectedServerId() {
        return details != null ? StringUtils.stripToNull(details.getArtifactoryName()) : null;
    }

    /**
     * Get a list of JFrog instances. Used in the Jelly to show the available servers to select.
     * If applicable, show the server selected in the pipeline.
     *
     * @return a list of JFrog Platform instances
     */
    public List<JFrogPlatformInstance> getJfrogInstances() {
        List<JFrogPlatformInstance> jfrogInstances = new ArrayList<>(RepositoriesUtils.getJFrogPlatformInstances());
        ArtifactoryServer propertyServer = getArtifactoryServerFromPipeline();
        if (propertyServer != null) {
            jfrogInstances.add(new JFrogPlatformInstance(propertyServer));
        }
        return jfrogInstances;
    }

    abstract static class ArtifactoryTriggerDescriptor extends TriggerDescriptor {

        @Nonnull
        public String getDisplayName() {
            return "Enable Artifactory trigger";
        }

        /**
         * Get a list of JFrog platform instance.
         * Used in the Jelly to show the available servers to select when the ArtifactoryTrigger instance is yet to be created.
         *
         * @return a list of JFrog Platform instances
         */
        public List<JFrogPlatformInstance> getJfrogInstances() {
            return RepositoriesUtils.getJFrogPlatformInstances();
        }
    }
}
