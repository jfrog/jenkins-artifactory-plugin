package org.jfrog.hudson.pipeline.integrationTests;

import hudson.FilePath;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class ITestUtils {

    static Path getResourcesDir() {
        return Paths.get("src", "test", "resources");
    }

    static String fixWindowsPath(String path) {
        return StringUtils.replace(path, "\\", "\\\\");
    }

    static boolean isExistInArtifactory(Artifactory artifactoryClient, String repo, String path) {
        RepositoryHandle repositoryHandle = artifactoryClient.repository(repo);
        try {
            repositoryHandle.file(path).info();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    static boolean isExistInWorkspace(JenkinsRule jenkins, WorkflowRun build, String dir, String fileName) throws IOException, InterruptedException {
        FilePath ws = jenkins.getInstance().getWorkspaceFor(build.getParent());
        if (ws == null) {
            throw new IOException("Workspace for " + build.getDisplayName() + " not found");
        }
        ws = ws.child(dir);
        if (!ws.exists()) {
            throw new IOException("Directory " + ws.getRemote() + " doesn't exist");
        }
        return ws.child(fileName).exists();
    }

    static void uploadFile(Artifactory artifactoryClient, Path source, String repo) {
        artifactoryClient.repository(repo).upload(source.getFileName().toString(), source.toFile()).doUpload();
    }
}
