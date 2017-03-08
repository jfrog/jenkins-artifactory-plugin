package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import org.acegisecurity.acls.NotFoundException;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by romang on 4/21/16.
 */
public class GetArtifactoryServerStep extends AbstractStepImpl {
    private String artifactoryServerID;

    @DataBoundConstructor
    public GetArtifactoryServerStep(String artifactoryServerID) {
        this.artifactoryServerID = artifactoryServerID;
    }

    public String getArtifactoryServerID() {
        return artifactoryServerID;
    }

    public static class Execution extends AbstractSynchronousStepExecution<org.jfrog.hudson.pipeline.types.ArtifactoryServer> {

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient GetArtifactoryServerStep step;

        @Override
        protected org.jfrog.hudson.pipeline.types.ArtifactoryServer run() throws Exception {
            String artifactoryServerID = step.getArtifactoryServerID();
            if (StringUtils.isEmpty(artifactoryServerID)) {
                getContext().onFailure(new MissingArgumentException("Artifactory server name is mandatory"));
            }

            List<ArtifactoryServer> artifactoryServers = new ArrayList<ArtifactoryServer>();
            List<ArtifactoryServer> artifactoryConfiguredServers = RepositoriesUtils.getArtifactoryServers();
            if (artifactoryConfiguredServers == null) {
                getContext().onFailure(new NotFoundException("No Artifactory servers were configured"));
            }
            for (ArtifactoryServer server : artifactoryConfiguredServers) {
                if (server.getName().equals(artifactoryServerID)) {
                    artifactoryServers.add(server);
                }
            }
            if (artifactoryServers.isEmpty()) {
                getContext().onFailure(new NotFoundException("Couldn't find Artifactory named: " + artifactoryServerID));
            }
            if (artifactoryServers.size() > 1) {
                throw new RuntimeException("Duplicate Artifactory name: " + artifactoryServerID);
            }
            ArtifactoryServer server = artifactoryServers.get(0);
            org.jfrog.hudson.pipeline.types.ArtifactoryServer artifactoryPipelineServer = new org.jfrog.hudson.pipeline.types.ArtifactoryServer(artifactoryServerID, server.getUrl(),
                    server.getResolvingCredentialsConfig().provideUsername(build.getParent()), server.getResolvingCredentialsConfig().providePassword(build.getParent()));
            artifactoryPipelineServer.setBypassProxy(server.isBypassProxy());
            artifactoryPipelineServer.getConnection().setRetry(server.getConnectionRetry());
            artifactoryPipelineServer.getConnection().setTimeout(server.getTimeout());
            return artifactoryPipelineServer;
        }

        private static final long serialVersionUID = 1L;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "getArtifactoryServer";
        }

        @Override
        public String getDisplayName() {
            return "Get Artifactory server from Jenkins config";
        }

        public ListBoxModel doFillArtifactoryServerIDItems() {
            return Utils.getServerListBox();
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
