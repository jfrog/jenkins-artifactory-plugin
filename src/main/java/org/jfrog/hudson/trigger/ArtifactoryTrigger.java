package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.build.client.ItemLastModified;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.ProxyUtils;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryTrigger extends Trigger<BuildableItem> {
    private static final Logger logger = Logger.getLogger(JenkinsBuildInfoLog.class.getName());
    private long lastModified = System.currentTimeMillis();
    private ServerDetails details;
    private final String paths;

    @DataBoundConstructor
    public ArtifactoryTrigger(String paths, String spec) throws ANTLRException {
        super(spec);
        this.paths = paths;
    }

    @DataBoundSetter
    public void setDetails(ServerDetails details) {
        this.details = details;
    }

    public ServerDetails getDetails() {
        return details;
    }

    public String getPaths() {
        return this.paths;
    }

    @Override
    public void run() {
        if (job == null) {
            return;
        }
        ArtifactoryServer server = getArtifactoryServer();

        try (ArtifactoryBuildInfoClient client = server.createArtifactoryClient(server.getDeployerCredentialsConfig().provideCredentials(job), ProxyUtils.createProxyConfiguration())) {
            String[] paths = this.paths.split(";");
            for (String path : paths) {
                ItemLastModified itemLastModified = client.getItemLastModified(StringUtils.trimToEmpty(path));
                long responseLastModified = itemLastModified.getLastModified();
                if (responseLastModified > lastModified) {
                    this.lastModified = responseLastModified;
                    logger.fine("Updating " + job.getName());
                    job.save();
                    job.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
                } else {
                    logger.fine(String.format("Artifactory trigger did not trigger job %s, since last modified time: %d is earlier or equal than %d for path %s", job.getName(), responseLastModified, lastModified, path));
                }
            }
        } catch (IOException | ParseException e) {
            logger.severe("Received an error: " + e.getMessage());
            logger.fine("Received an error: " + e);
        }
    }

    @Override
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
     * Get Artifactory server from the global configuration by server ID. Throw RuntimeException if not exist.
     *
     * @param serverId - The server ID to look
     * @return artifactory server
     */
    private ArtifactoryServer getGlobalArtifactoryServer(String serverId) {
        ArtifactoryServer server = RepositoriesUtils.getArtifactoryServer(serverId, RepositoriesUtils.getArtifactoryServers());
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

    /**
     * Get the selected server id. Used in the Jelly to show the selected server.
     *
     * @return the server ID
     */
    public String getSelectedServerId() {
        return details != null ? StringUtils.stripToNull(details.getArtifactoryName()) : null;
    }

    /**
     * Get a list of Artifactory servers. Used in the Jelly to show the available servers to select.
     * If applicable, show the server selected in the pipeline.
     *
     * @return a list of Artifactory servers
     */
    public List<ArtifactoryServer> getArtifactoryServers() {
        List<ArtifactoryServer> servers = new ArrayList<>(RepositoriesUtils.getArtifactoryServers());
        ArtifactoryServer propertyServer = getArtifactoryServerFromPipeline();
        if (propertyServer != null) {
            servers.add(propertyServer);
        }
        return servers;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends TriggerDescriptor {

        public String getDisplayName() {
            return "Enable Artifactory trigger";
        }

        public boolean isApplicable(Item item) {
            return item instanceof BuildableItem;
        }

        /**
         * Get a list of Artifactory servers.
         * Used in the Jelly to show the available servers to select when the ArtifactoryTrigger instance is yet to be created.
         *
         * @return a list of Artifactory servers
         */
        public List<ArtifactoryServer> getArtifactoryServers() {
            return RepositoriesUtils.getArtifactoryServers();
        }
    }
}