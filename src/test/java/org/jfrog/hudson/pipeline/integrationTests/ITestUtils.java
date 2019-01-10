package org.jfrog.hudson.pipeline.integrationTests;

import hudson.FilePath;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.artifactory.client.model.LightweightRepository;
import org.jfrog.artifactory.client.model.RepoPath;
import org.jfrog.artifactory.client.model.RepositoryType;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl.*;
import static org.junit.Assert.*;

class ITestUtils {

    static Path getIntegrationDir() {
        return Paths.get("src", "test", "resources", "integration");
    }

    static String fixWindowsPath(String path) {
        return StringUtils.replace(path, "\\", "\\\\");
    }

    static void cleanUpArtifactory(Artifactory artifactoryClient) {
        Arrays.asList(LOCAL, REMOTE, VIRTUAL).forEach(repoType -> cleanUpRepositoryType(artifactoryClient, repoType));
    }

    private static void cleanUpRepositoryType(Artifactory artifactoryClient, RepositoryType repositoryType) {
        artifactoryClient.repositories().list(repositoryType).stream()
                .map(LightweightRepository::getKey)
                .filter(repoKey -> org.apache.commons.lang3.StringUtils.startsWithAny(repoKey, TestRepository.toArray()))
                .forEach(repoKey -> artifactoryClient.repository(repoKey).delete());
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

    static void assertNoArtifactsInRepo(Artifactory artifactoryClient, String repoKey) {
        List<RepoPath> repoPaths = artifactoryClient.searches().repositories(repoKey).artifactsByName("*in").doSearch();
        assertTrue(repoPaths.isEmpty());
    }

    static void assertArtifactsInRepo(Artifactory artifactoryClient, String repoKey, Set<String> expectedArtifacts) {
        List<RepoPath> repoPaths = artifactoryClient.searches().repositories(repoKey).artifactsByName("*in").doSearch();
        Set<String> actualArtifacts = repoPaths.stream().map(RepoPath::getItemPath).collect(Collectors.toSet());
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
        artifactoryClient.restCall(new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.DELETE)
                .apiUrl("api/build/" + buildName)
                .addQueryParam("deleteAll", "1"));
    }
}
