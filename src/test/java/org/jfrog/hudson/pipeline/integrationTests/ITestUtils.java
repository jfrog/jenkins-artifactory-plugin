package org.jfrog.hudson.pipeline.integrationTests;

import hudson.FilePath;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

class ITestUtils {

    static Path getIntegrationDir() {
        return Paths.get("src", "test", "resources", "integration");
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

    static Build getBuildInfo(ArtifactoryBuildInfoClient buildInfoClient, String buildName, String buildNumber) throws IOException {
        return buildInfoClient.getBuildInfo(buildName, buildNumber);
    }

    static void assertModuleDependencies(Module module, Set<String> expectedDependencies) {
        Set<String> actualDependencies = module.getDependencies().stream().map(Dependency::getId).collect(Collectors.toSet());
        assertEquals(expectedDependencies, actualDependencies);
    }

    static void assertModuleArtifacts(Module module, Set<String> expectedArtifacts) {
        Set<String> actualArtifacts = module.getArtifacts().stream().map(Artifact::getName).collect(Collectors.toSet());
        assertEquals(expectedArtifacts, actualArtifacts);
    }

    static Module getAndAssertModule(Build buildInfo, String moduleName) {
        assertNotNull(buildInfo);
        assertNotNull(buildInfo.getModules());
        assertEquals(1, buildInfo.getModules().size());
        Module module = buildInfo.getModule(moduleName);
        assertNotNull(module);
        return module;
    }

    static void deleteBuild(Artifactory artifactoryClient, String buildName) throws IOException {
        artifactoryClient.restCall(new ArtifactoryRequestImpl().method(ArtifactoryRequest.Method.DELETE).apiUrl("api/build/" + buildName).addQueryParam("deleteAll", "1"));
    }
}
