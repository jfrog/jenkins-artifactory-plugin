package org.jfrog.hudson;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds an Artifactory server `JX_ARTIFACTORY_SERVER` to servers list on startup if `artifactoryUrl`, `artifactoryUsername` and `artifactoryPassword` files exist on `/var/artifactory/`.
 * When the Jenkins instance is created by Jenkins X, a default Artifactory server configuration should be created. This class creates this server.
 */
@Extension
public class JenkinsXArtifactoryBuilderListener extends ComputerListener implements Serializable {
    private static final String JX_ARTIFACTORY_SERVER_ID = "JX_ARTIFACTORY_SERVER";

    @Override
    public void onOnline(final Computer c, TaskListener listener) throws IOException, InterruptedException {
        ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl) Jenkins.getInstance().getDescriptor(ArtifactoryBuilder.class);
        List<ArtifactoryServer> artifactoryServers = descriptor.getArtifactoryServers();
        if (artifactoryServers == null) {
            artifactoryServers = new ArrayList<>();
        }
        if (!isServerExists(artifactoryServers)) {
            Path secretsDir = Paths.get("/var", "artifactory");
            File artifactoryUrl = secretsDir.resolve("artifactoryUrl").toFile();
            File artifactoryUsername = secretsDir.resolve("artifactoryUsername").toFile();
            File artifactoryPassword = secretsDir.resolve("artifactoryPassword").toFile();
            if (artifactoryUrl.isFile() && artifactoryUsername.isFile() && artifactoryPassword.isFile()) {
                String artifactoryUrlStr = FileUtils.readFileToString(artifactoryUrl);
                String artifactoryUsernameStr = FileUtils.readFileToString(artifactoryUsername);
                String artifactoryPasswordStr = FileUtils.readFileToString(artifactoryPassword);
                artifactoryServers.add(createArtifactoryServer(artifactoryUrlStr, artifactoryUsernameStr, artifactoryPasswordStr));
                descriptor.setArtifactoryServers(artifactoryServers);
                descriptor.save();
            }
        }

        super.onOnline(c, listener);
    }

    private ArtifactoryServer createArtifactoryServer(String url, String username, String password) {
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, "");
        return new ArtifactoryServer(JX_ARTIFACTORY_SERVER_ID, url, credentialsConfig, credentialsConfig, 0, false, null);
    }

    private boolean isServerExists(List<ArtifactoryServer> artifactoryServers) {
        for (ArtifactoryServer server : artifactoryServers) {
            if (JX_ARTIFACTORY_SERVER_ID.equals(server.getName())) {
                return true;
            }
        }
        return false;
    }
}
