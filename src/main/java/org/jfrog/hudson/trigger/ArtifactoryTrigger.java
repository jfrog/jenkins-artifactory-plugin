package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.*;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.ItemLastModified;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryTrigger extends Trigger {
    private static final Logger logger = Logger.getLogger(JenkinsBuildInfoLog.class.getName());

    private String path;
    private ServerDetails details;
    private long lastModified = System.currentTimeMillis();

    @DataBoundConstructor
    public ArtifactoryTrigger(String path, String spec, ServerDetails details) throws ANTLRException {
        super(spec);
        this.path = path;
        this.details = details;
    }

    @Override
    public void run() {
        if (job == null) {
            return;
        }

        ArtifactoryServer artifactoryServer = RepositoriesUtils.getArtifactoryServer(details.getArtifactoryName(), RepositoriesUtils.getArtifactoryServers());
        if (artifactoryServer == null) {
            logger.warning("Artifactory server " + details.getArtifactoryName() + " doesn't exists.");
            return;
        }
        ArtifactoryHttpClient client = new ArtifactoryHttpClient(artifactoryServer.getUrl(),
                artifactoryServer.getDeployerCredentialsConfig().provideUsername(job),
                artifactoryServer.getDeployerCredentialsConfig().providePassword(job),
                new NullLog());
        try {
            ItemLastModified itemLastModified = client.getItemLastModified(path);
                long responseLastModified = itemLastModified.getLastModified();
                if (responseLastModified > lastModified) {
                    this.lastModified = responseLastModified;
                    if (job instanceof Project) {
                        AbstractProject<?, ?> project = ((Project) job).getRootProject();
                        logger.fine("Updating " + job.getName());
                        project.save();
                        project.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
                        return;
                    }

                    if (job instanceof WorkflowJob) {
                        WorkflowJob project = (WorkflowJob) job;
                        logger.fine("Updating " + job.getName());
                        project.save();
                        project.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
                    }
                } else {
                    logger.fine(job.getName() + " job received last modified time that is not newer for " + path);
                }
        } catch (IOException | ParseException e) {
            logger.severe("Received an error: " + e.getMessage());
            logger.fine("Received an error: " + e);
        }
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

    private long getLastModified(String date) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        Date parse = simpleDateFormat.parse(date);
        return parse.getTime();
    }

    @Extension
    public static final class DescriptorImpl extends TriggerDescriptor {

        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        public String getDisplayName() {
            return "Enable Artifactory trigger";
        }

        public boolean isApplicable(Item item) {
            return true;
        }

        public List<ArtifactoryServer> getArtifactoryServers() {
            return RepositoriesUtils.getArtifactoryServers();
        }
    }
}