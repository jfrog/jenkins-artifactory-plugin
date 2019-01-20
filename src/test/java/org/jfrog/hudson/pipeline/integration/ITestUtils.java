package org.jfrog.hudson.pipeline.integration;

import hudson.FilePath;
import org.apache.commons.collections.CollectionUtils;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl.*;
import static org.junit.Assert.*;

/**
 * @author yahavi
 */
class ITestUtils {

    /**
     * Get the integration tests dir.
     *
     * @return integration tests dir
     */
    static Path getIntegrationDir() {
        return Paths.get("src", "test", "resources", "integration");
    }

    /**
     * Escape backslashes in filesystem path.
     *
     * @param path - Filesystem path to fix
     * @return path compatible with Windows
     */
    static String fixWindowsPath(String path) {
        return StringUtils.replace(path, "\\", "\\\\");
    }

    /**
     * Clean up old test repositories.
     *
     * @param artifactoryClient - The Artifactory java client
     */
    static void cleanUpArtifactory(Artifactory artifactoryClient) {
        Arrays.asList(LOCAL, REMOTE, VIRTUAL).forEach(repoType -> cleanUpRepositoryType(artifactoryClient, repoType));
    }

    /**
     * Clean up old tests repositories with the specified type - Local, Remote or Virtual
     *
     * @param artifactoryClient - The Artifactory java client
     * @param repositoryType    - The repository type to delete
     */
    private static void cleanUpRepositoryType(Artifactory artifactoryClient, RepositoryType repositoryType) {
        artifactoryClient.repositories().list(repositoryType).stream()
                .map(LightweightRepository::getKey)
                .filter(repoKey -> org.apache.commons.lang3.StringUtils.startsWithAny(repoKey, TestRepository.toArray()))
                .forEach(repoKey -> artifactoryClient.repository(repoKey).delete());
    }

    /**
     * Return true if the Artifact exists in the repository.
     *
     * @param artifactoryClient - Artifactory java client
     * @param repoKey           - Repository key
     * @param artifactName      - Artifact name
     * @return true if the artifact exists in the repository
     */
    static boolean isExistInArtifactory(Artifactory artifactoryClient, String repoKey, String artifactName) {
        RepositoryHandle repositoryHandle = artifactoryClient.repository(repoKey);
        try {
            repositoryHandle.file(artifactName).info();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Return true if the file exists in the workspace under the input directory.
     *
     * @param jenkins  - Jenkins instance
     * @param build    - Jenkins job
     * @param dir      - Directory under workspace
     * @param fileName - File name to search
     * @return true if the file exists in the workspace under the input directory
     */
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

    /**
     * Use Artifactory java client to upload a file to Artifactory.
     *
     * @param artifactoryClient - Artifactory java client
     * @param source            - Source file to upload
     * @param repoKey           - Repository key
     */
    static void uploadFile(Artifactory artifactoryClient, Path source, String repoKey) {
        artifactoryClient.repository(repoKey).upload(source.getFileName().toString(), source.toFile()).doUpload();
    }

    /**
     * Get build info from Artifactory.
     *
     * @param buildInfoClient - Artifactory build-info client
     * @param buildName       - Build name
     * @param buildNumber     - Build number
     * @return build info for the specified build name and number
     */
    static Build getBuildInfo(ArtifactoryBuildInfoClient buildInfoClient, String buildName, String buildNumber) throws IOException {
        return buildInfoClient.getBuildInfo(encodeBuildName(buildName), buildNumber);
    }

    /**
     * Assert that the module dependencies and the expected dependencies are equal.
     *
     * @param module               - Module to check
     * @param expectedDependencies - Expected dependencies
     */
    static void assertModuleDependencies(Module module, Set<String> expectedDependencies) {
        Set<String> actualDependencies = module.getDependencies().stream().map(Dependency::getId).collect(Collectors.toSet());
        assertEquals(expectedDependencies, actualDependencies);
    }

    /**
     * Assert that the module artifacts and the expected artifacts are equal.
     *
     * @param module            - Module to check
     * @param expectedArtifacts - Expected artifacts
     */
    static void assertModuleArtifacts(Module module, Set<String> expectedArtifacts) {
        Set<String> actualArtifacts = module.getArtifacts().stream().map(Artifact::getName).collect(Collectors.toSet());
        assertEquals(expectedArtifacts, actualArtifacts);
    }

    /**
     * Assert no artifacts in repository.
     *
     * @param artifactoryClient - Artifactory java client
     * @param repoKey           - Repository key
     */
    static void assertNoArtifactsInRepo(Artifactory artifactoryClient, String repoKey) {
        List<RepoPath> repoPaths = artifactoryClient.searches().repositories(repoKey).artifactsByName("*in").doSearch();
        assertTrue(repoPaths.isEmpty());
    }

    /**
     * Assert artifacts exist in repository.
     *
     * @param artifactoryClient - Artifactory java client
     * @param repoKey           - Repository key
     * @param expectedArtifacts - Expected artifacts
     */
    static void assertArtifactsInRepo(Artifactory artifactoryClient, String repoKey, Set<String> expectedArtifacts) {
        List<RepoPath> repoPaths = artifactoryClient.searches().repositories(repoKey).artifactsByName("*in").doSearch();
        Set<String> actualArtifacts = repoPaths.stream().map(RepoPath::getItemPath).collect(Collectors.toSet());
        assertEquals(expectedArtifacts, actualArtifacts);
    }

    /**
     * Get module from the build-info object and assert its existence.
     *
     * @param buildInfo  - Build-info object
     * @param moduleName - Module name
     * @return module from the build-info
     */
    static Module getAndAssertModule(Build buildInfo, String moduleName) {
        assertNotNull(buildInfo);
        assertNotNull(buildInfo.getModules());
        Module module = buildInfo.getModule(moduleName);
        assertNotNull(module);
        return module;
    }

    /**
     * Assert that the artifacts and dependencies lists are not empty in the input module.
     *
     * @param buildInfo  - Build info object
     * @param moduleName - Module name
     */
    static void assertModuleContainsArtifactsAndDependencies(Build buildInfo, String moduleName) {
        Module module = getAndAssertModule(buildInfo, moduleName);
        assertTrue(CollectionUtils.isNotEmpty(module.getArtifacts()));
        assertTrue(CollectionUtils.isNotEmpty(module.getDependencies()));
    }

    /**
     * Assert that the artifacts list is not empty in the input module.
     *
     * @param buildInfo  - Build info object
     * @param moduleName - Module name
     */
    static void assertModuleContainsArtifacts(Build buildInfo, String moduleName) {
        Module module = getAndAssertModule(buildInfo, moduleName);
        assertTrue(CollectionUtils.isNotEmpty(module.getArtifacts()));
    }

    /**
     * Delete build in Artifactory.
     *
     * @param artifactoryClient - Artifactory java client
     * @param buildName         - Build name to delete
     */
    static void deleteBuild(Artifactory artifactoryClient, String buildName) throws IOException {
        artifactoryClient.restCall(new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.DELETE)
                .apiUrl("api/build/" + encodeBuildName(buildName))
                .addQueryParam("deleteAll", "1")
                .addQueryParam("artifacts", "1"));
    }

    private static String encodeBuildName(String buildName) throws UnsupportedEncodingException {
        return URLEncoder.encode(buildName, "UTF-8").replace("+", "%20");
    }
}
