package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.HashMap;
import java.util.Map;

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

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

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
