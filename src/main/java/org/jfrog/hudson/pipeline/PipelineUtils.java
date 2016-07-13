package org.jfrog.hudson.pipeline;

import hudson.model.Run;
import hudson.util.ListBoxModel;
import org.apache.commons.cli.MissingArgumentException;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.BuildInfo;
import org.jfrog.hudson.util.RepositoriesUtils;

import java.util.List;

/**
 * Created by romang on 4/24/16.
 */
public class PipelineUtils {

    public static final String BUILD_INFO_DELIMITER = ".";

    /**
     * Prepares Artifactory server either from serverID or from ArtifactoryServer.
     *
     * @param artifactoryServerID
     * @param pipelineServer
     * @return
     */
    public static org.jfrog.hudson.ArtifactoryServer prepareArtifactoryServer(String artifactoryServerID,
                                                                              ArtifactoryServer pipelineServer) throws MissingArgumentException {

        if (artifactoryServerID == null && pipelineServer == null) {
            return null;
        }
        if (artifactoryServerID != null && pipelineServer != null) {
            return null;
        }
        if (pipelineServer != null) {
            CredentialsConfig credentials = pipelineServer.getCredentials();

            return new org.jfrog.hudson.ArtifactoryServer(null, pipelineServer.getUrl(), credentials,
                    credentials, 0, pipelineServer.isBypassProxy());
        }
        org.jfrog.hudson.ArtifactoryServer server = RepositoriesUtils.getArtifactoryServer(artifactoryServerID, RepositoriesUtils.getArtifactoryServers());
        if (server == null) {
            return null;
        }
        return server;
    }

    public static ListBoxModel getServerListBox() {
        ListBoxModel r = new ListBoxModel();
        List<org.jfrog.hudson.ArtifactoryServer> servers = RepositoriesUtils.getArtifactoryServers();
        r.add("", "");
        for (org.jfrog.hudson.ArtifactoryServer server : servers) {
            r.add(server.getName() + PipelineUtils.BUILD_INFO_DELIMITER + server.getUrl(), server.getName());
        }
        return r;
    }

    public static BuildInfo prepareBuildinfo(Run build, BuildInfo buildinfo) {
        if (buildinfo == null) {
            return new BuildInfo(build);
        }
        return buildinfo;
    }
}
