package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by romang on 4/21/16.
 */
public class CreateArtifactoryServerStep extends AbstractStepImpl {
    private String url;
    private String username;
    private String password;
    private String credentialsId;

    @DataBoundConstructor
    public CreateArtifactoryServerStep(String url, String username, String password, String credentialsId) {
        this.url = url;
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

    public static class Execution extends AbstractSynchronousStepExecution<ArtifactoryServer> {
        private static final long serialVersionUID = 1L;

        @Inject(optional = true)
        private transient CreateArtifactoryServerStep step;

        @Override
        protected ArtifactoryServer run() throws Exception {
            String artifactoryUrl = step.getUrl();
            if (StringUtils.isEmpty(artifactoryUrl)) {
                getContext().onFailure(new MissingArgumentException("Artifactory server URL is mandatory"));
            }
            if (!StringUtils.isEmpty(step.getCredentialsId())) {
                return new ArtifactoryServer(artifactoryUrl, step.getCredentialsId());
            }
            return new ArtifactoryServer(artifactoryUrl, step.getUsername(), step.getPassword());
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(CreateArtifactoryServerStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "newArtifactoryServer";
        }

        @Override
        public String getDisplayName() {
            return "Returns new Artifactory server";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
