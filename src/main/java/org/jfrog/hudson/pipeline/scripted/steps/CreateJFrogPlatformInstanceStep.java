package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Created by romang on 4/21/16.
 */
public class CreateJFrogPlatformInstanceStep extends AbstractStepImpl {
    private final String distributionUrl;
    private final String artifactoryUrl;
    private final String credentialsId;
    private final String username;
    private final String password;
    private final String url;

    @DataBoundConstructor
    public CreateJFrogPlatformInstanceStep(String url, String artifactoryUrl, String distributionUrl, String username, String password, String credentialsId) {
        this.url = url;
        this.artifactoryUrl = artifactoryUrl;
        this.distributionUrl = distributionUrl;
        this.username = username;
        this.password = password;
        this.credentialsId = credentialsId;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * We don't use additional context fields in this step execution,
     * so we extend SynchronousStepExecution directly and not ArtifactorySynchronousStepExecution
     */
    public static class Execution extends SynchronousStepExecution<JFrogPlatformInstance> {
        private static final long serialVersionUID = 1L;

        private transient CreateJFrogPlatformInstanceStep step;

        @Inject
        public Execution(CreateJFrogPlatformInstanceStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected JFrogPlatformInstance run() throws Exception {
            if (isAllBlank(step.url, step.artifactoryUrl, step.distributionUrl)) {
                throw new IllegalArgumentException("At least one of the following is mandatory: 'url', 'artifactoryUrl', 'distributionUrl'");
            }
            String artifactoryUrl = defaultIfBlank(step.artifactoryUrl, appendIfMissing(step.url, "/") + "artifactory");
            String distributionUrl = defaultIfBlank(step.distributionUrl, appendIfMissing(step.url, "/") + "distribution");

            ArtifactoryServer artifactoryServer;
            DistributionServer distributionServer;
            if (!isEmpty(step.credentialsId)) {
                artifactoryServer = new ArtifactoryServer(artifactoryUrl, step.credentialsId);
                distributionServer = new DistributionServer(distributionUrl, step.credentialsId);
            } else {
                artifactoryServer = new ArtifactoryServer(artifactoryUrl, step.username, step.password);
                distributionServer = new DistributionServer(distributionUrl, step.username, step.password);
            }
            artifactoryServer.setPlatformUrl(step.url);
            return new JFrogPlatformInstance(artifactoryServer, distributionServer, step.url, "");
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateJFrogPlatformInstanceStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newJFrogPlatformInstance";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Returns new JFrog platform instance";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
