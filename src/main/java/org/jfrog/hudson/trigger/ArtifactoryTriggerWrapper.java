package org.jfrog.hudson.trigger;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.*;
import hudson.triggers.SCMTrigger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.ArtifactoryItemLastModified;
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
public class ArtifactoryTriggerWrapper extends SCMTrigger {
    private static final Logger logger = Logger.getLogger(JenkinsBuildInfoLog.class.getName());

    private String path;
    private ServerDetails details;
    private long lastModified;

    @DataBoundConstructor
    public ArtifactoryTriggerWrapper(String path, String spec, ServerDetails details) throws ANTLRException {
        super(spec);
        this.path = path;
        this.details = details;
        this.lastModified = System.currentTimeMillis();
    }

    @Override
    public void run() {
        if (job == null) {
            return;
        }

        ArtifactoryServer artifactoryServer = RepositoriesUtils.getArtifactoryServer(details.getArtifactoryName(), RepositoriesUtils.getArtifactoryServers());
        ArtifactoryHttpClient client = new ArtifactoryHttpClient(artifactoryServer.getUrl(), artifactoryServer.getDeployerCredentialsConfig().provideUsername(job), artifactoryServer.getDeployerCredentialsConfig().providePassword(job), new NullLog());
        try {
            ArtifactoryItemLastModified itemLastModified = client.getItemLastModified(path);
            if (itemLastModified != null) {
                long artifactoryResponseLastModified = getLastModified(itemLastModified.getLastModified());
                if (artifactoryResponseLastModified > lastModified) {
                    this.lastModified = artifactoryResponseLastModified;
                    if (job instanceof Project) {
                        AbstractProject<?, ?> project = ((Project) job).getRootProject();
                        logger.fine("Updating " + job.getName());
                        project.save();
                        project.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
                        return;
                    }

                    if (job instanceof WorkflowJob) {
                        WorkflowJob project = (WorkflowJob) job;
                        project.save();
                        logger.fine("Updating " + job.getName());
                        project.scheduleBuild(new ArtifactoryCause(itemLastModified.getUri()));
                    }
                }
            }
        } catch (IOException | ParseException e) {
            logger.severe("Received an error:" + e.getMessage());
            logger.fine("Received an error:" + e);
        }
    }

    @Override
    public void start(Item project, boolean newInstance) {
        super.start(project, newInstance);
    }

    @Override
    public void run(Action[] additionalActions) {
        super.run(additionalActions);
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
    public static final class DescriptorImpl extends SCMTrigger.DescriptorImpl {

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