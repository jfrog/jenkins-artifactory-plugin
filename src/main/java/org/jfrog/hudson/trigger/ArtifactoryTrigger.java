package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ItemLastModified;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.*;
import org.jfrog.hudson.util.*;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryTrigger extends Trigger implements DeployerOverrider {
    private static final Logger logger = Logger.getLogger(JenkinsBuildInfoLog.class.getName());

    private String path;
    private ServerDetails details;
    private long lastModified = System.currentTimeMillis();
    private final CredentialsConfig deployerCredentialsConfig;

    /**
     * @deprecated: Use org.jfrog.hudson.generic.ArtifactoryGenericConfigurator#getDeployerCredentials()()
     */
    @Deprecated
    private Credentials overridingDeployerCredentials;

    @DataBoundConstructor
    public ArtifactoryTrigger(String path, String spec, ServerDetails details, CredentialsConfig deployerCredentialsConfig) throws ANTLRException {
        super(spec);
        this.path = path;
        this.details = details;
        this.deployerCredentialsConfig = deployerCredentialsConfig;
    }

    @Override
    public void run() {
        if (job == null) {
            return;
        }

        ArtifactoryServer server = RepositoriesUtils.getArtifactoryServer(details.getArtifactoryName(), RepositoriesUtils.getArtifactoryServers());
        if (server == null) {
            logger.warning("Artifactory Trigger failed triggering the job, since Artifactory server " + details.getArtifactoryName() + " does not exist.");
            return;
        }

        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(this, server);
        try (ArtifactoryBuildInfoClient client = server.createArtifactoryClient(
                preferredDeployer.provideUsername(job),
                preferredDeployer.providePassword(job),
                server.createProxyConfiguration(Jenkins.getInstance().proxy),
                new NullLog())) {
            ItemLastModified itemLastModified = client.getItemLastModified(path);
            long responseLastModified = itemLastModified.getLastModified();
            if (responseLastModified > lastModified) {
                this.lastModified = responseLastModified;
                if (job instanceof Project) {
                    AbstractProject<?, ?> project = ((Project) job).getRootProject();
                    saveAndSchedule(itemLastModified, project);
                    return;
                }

                if (job instanceof MavenModuleSet) {
                    AbstractProject project = ((MavenModuleSet)job).getRootProject();
                    saveAndSchedule(itemLastModified, project);
                    return;
                }

                if (job instanceof WorkflowJob) {
                    WorkflowJob project = (WorkflowJob) job;
                    logger.fine("Updating " + job.getName());
                    project.save();
                    project.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
                }
            } else {
                logger.fine(String.format("Artifactory trigger did not trigger job %s, since last modified time: %d is earlier or equal than %d for path %s", job.getName(), responseLastModified, lastModified, path));
            }
        } catch (IOException | ParseException e) {
            logger.severe("Received an error: " + e.getMessage());
            logger.fine("Received an error: " + e);
        }
    }

    private void saveAndSchedule(ItemLastModified itemLastModified, AbstractProject project) throws IOException {
        logger.fine("Updating " + job.getName());
        project.save();
        project.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
    }

    @Override
    public void stop() {
        if (job != null) {
            logger.info("Stopping " + job.getName() + " Artifactory trigger.");
        }
        super.stop();
    }

    public String getPath() {
        return path;
    }

    public ServerDetails getDetails() {
        return details;
    }

    public String getArtifactoryName() {
        return getDetails() != null ? getDetails().artifactoryName : null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean isOverridingDefaultDeployer() {
        return deployerCredentialsConfig != null && deployerCredentialsConfig.isCredentialsProvided();
    }

    @Override
    public Credentials getOverridingDeployerCredentials() {
        return overridingDeployerCredentials;
    }

    @Override
    public CredentialsConfig getDeployerCredentialsConfig() {
        return deployerCredentialsConfig;
    }

    @Extension
    public static final class DescriptorImpl extends TriggerDescriptor {

        public String getDisplayName() {
            return "Enable Artifactory trigger";
        }

        public boolean isApplicable(Item item) {
            return (item instanceof WorkflowJob || item instanceof Project || item instanceof MavenModuleSet);
        }

        public List<ArtifactoryServer> getArtifactoryServers() {
            return RepositoriesUtils.getArtifactoryServers();
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            return PluginsUtils.fillPluginCredentials(project);
        }
    }
}