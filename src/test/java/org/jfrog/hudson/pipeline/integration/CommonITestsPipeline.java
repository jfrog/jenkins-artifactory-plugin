package org.jfrog.hudson.pipeline.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import hudson.EnvVars;
import hudson.model.Result;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.buildScanTable.BuildScanTableHelper;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DeleteReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.GetReleaseBundleStatusResponse;
import org.jfrog.build.extractor.docker.DockerJavaWrapper;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.trigger.ArtifactoryTrigger;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.isOneOf;
import static org.jfrog.hudson.TestUtils.getAndAssertChild;
import static org.jfrog.hudson.pipeline.common.executors.GenericDownloadExecutor.FAIL_NO_OP_ERROR_MESSAGE;
import static org.jfrog.hudson.pipeline.integration.ITestUtils.*;
import static org.jfrog.hudson.util.SerializationUtils.createMapper;
import static org.junit.Assert.*;

/**
 * @author yahavi
 */
@SuppressWarnings("UnconstructableJUnitTestCase")
public class CommonITestsPipeline extends PipelineTestBase {

    CommonITestsPipeline(PipelineType pipelineType) {
        super(pipelineType);
    }

    void downloadByPatternTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        WorkflowRun pipelineResults = null;

        Files.list(FILES_PATH).filter(Files::isRegularFile)
                .forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        try {
            pipelineResults = runPipeline("downloadByPattern", false);
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, pipelineResults, "downloadByPattern-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void downloadByAqlTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        WorkflowRun pipelineResults = null;

        Files.list(FILES_PATH).filter(Files::isRegularFile)
                .forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        try {
            pipelineResults = runPipeline("downloadByAql", false);
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, pipelineResults, "downloadByAql-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void downloadByPatternAndBuildTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a.in");
        String buildNumber = BUILD_NUMBER + "-3";
        WorkflowRun pipelineResults = null;

        Set<String> unexpected = getTestFilesNamesByLayer(0);
        unexpected.addAll(getTestFilesNamesByLayer(1));
        unexpected.removeAll(expectedDependencies);
        try {
            pipelineResults = runPipeline("downloadByPatternAndBuild", false);
            assertTrue(isExistInWorkspace(slave, pipelineResults, "downloadByPatternAndBuild-test", "a.in"));
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, pipelineResults, "downloadByPatternAndBuild-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, buildNumber, null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER + "-1", BUILD_NUMBER + "-2", BUILD_NUMBER + "-3");
        }
    }

    void downloadByBuildOnlyTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        Set<String> unexpected = getTestFilesNamesByLayer(1);
        WorkflowRun pipelineResults = null;

        try {
            pipelineResults = runPipeline("downloadByBuildOnly", false);
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, pipelineResults, "downloadByBuildOnly-test", fileName));
            }
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, pipelineResults, "downloadByBuildOnly-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER + "-3", null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER + "-1", BUILD_NUMBER + "-2", BUILD_NUMBER + "-3");
        }
    }

    void downloadNonExistingBuildTest(String buildName) throws Exception {
        boolean success = false;
        try {
            runPipeline("downloadNonExistingBuild", false);
            fail("Job expected to fail");
        } catch (AssertionError t) {
            if (t.getMessage().contains(FAIL_NO_OP_ERROR_MESSAGE)) {
                success = true;
            } else {
                fail("Job expected error message:'" + FAIL_NO_OP_ERROR_MESSAGE + "' but actual got:" + t.getMessage());
            }
        } finally {
            if (success) {
                artifactoryManager.deleteBuilds(buildName, null, true, BUILD_NUMBER);
                cleanOldBuilds(buildName, null);
            }
        }
    }

    /**
     * Upload a file to 2 different builds.
     * Verify that we don't download files with same sha and different build name and build number.
     */
    void downloadByShaAndBuildTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a3");
        Set<String> unexpected = Sets.newHashSet("a4", "a5");
        WorkflowRun pipelineResults = null;

        try {
            pipelineResults = runPipeline("downloadByShaAndBuild", false);
            // Only a.in should be in workspace
            assertTrue(isExistInWorkspace(slave, pipelineResults, "downloadByShaAndBuild-test", "a3"));
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, pipelineResults, "downloadByShaAndBuild-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER + "-4", null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER + "-1", BUILD_NUMBER + "-4");
            cleanupBuilds(pipelineResults, buildName + "-second", null, BUILD_NUMBER + "-2", BUILD_NUMBER + "-3");
        }
    }

    /**
     * Upload a file to 2 different builds.
     * Verify that we don't download files with same sha and build name and different build number.
     */
    void downloadByShaAndBuildNameTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a4");
        Set<String> unexpected = Sets.newHashSet("a3", "a5");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("downloadByShaAndBuildName", false);
            // Only a.in should be in workspace
            assertTrue(isExistInWorkspace(slave, pipelineResults, "downloadByShaAndBuildName-test", "a4"));
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, pipelineResults, "downloadByShaAndBuildName-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER + "-4", null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER + "-1", BUILD_NUMBER + "-2", BUILD_NUMBER + "-4");
            cleanupBuilds(pipelineResults, buildName + "-second", null, BUILD_NUMBER + "-3");
        }
    }

    void uploadTest(String buildName, String project, String pipelineName) throws Exception {
        Set<String> expectedArtifacts = getTestFilesNamesByLayer(0);
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineName, false);
            expectedArtifacts.forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));
            Build buildInfo = getBuildInfo(artifactoryManager, buildName, BUILD_NUMBER, project);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleArtifacts(module, expectedArtifacts);
        } finally {
            cleanupBuilds(pipelineResults, buildName, project, BUILD_NUMBER);
        }
    }

    void uploadDownloadCustomModuleNameTest(String buildName) throws Exception {
        Set<String> expectedArtifactsAndDependencies = getTestFilesNamesByLayer(0);
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("uploadDownloadCustomModuleName", false);
            expectedArtifactsAndDependencies.forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));
            for (String fileName : expectedArtifactsAndDependencies) {
                assertTrue(isExistInWorkspace(slave, pipelineResults, "downloadByPattern-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            Module module = getAndAssertModule(buildInfo, "my-generic-module");
            assertModuleDependencies(module, expectedArtifactsAndDependencies);
            assertModuleArtifacts(module, expectedArtifactsAndDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void uploadWithPropsTest() throws Exception {
        Set<String> uploadFiles = Sets.newHashSet("a.in", "b.in", "c.in");
        WorkflowRun build = runPipeline("uploadWithProps", false);
        for (String fileName : uploadFiles) {
            assertTrue(fileName + " doesn't exist locally", isExistInWorkspace(slave, build, "UploadWithProps-test", fileName));
        }
    }

    void promotionTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        WorkflowRun pipelineResults = null;
        Files.list(FILES_PATH).filter(Files::isRegularFile)
                .forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        try {
            pipelineResults = runPipeline("promote", false);
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, pipelineResults, "promotion-test", fileName));
            }
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
            // In this tests, the expected dependencies and artifacts are equal
            assertModuleArtifacts(module, expectedDependencies);
            assertNoArtifactsInRepo(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1));
            assertArtifactsInRepo(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO2), expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void mavenTest(String buildName, boolean useWrapper) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet("multi-3.7-SNAPSHOT.pom");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(useWrapper ? "mavenWrapper" : "maven", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            assertEquals(4, buildInfo.getModules().size());

            Module module = getAndAssertModule(buildInfo, "org.jfrog.test:multi:3.7-SNAPSHOT");
            assertModuleArtifacts(module, expectedArtifacts);
            assertTrue(CollectionUtils.isEmpty(module.getDependencies()));
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi1:3.7-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi2:3.7-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi3:3.7-SNAPSHOT");
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void mavenJibTest(String buildName) throws Exception {
        Assume.assumeFalse("Skipping Docker tests", SystemUtils.IS_OS_WINDOWS);
        Set<String> expectedArtifacts = Sets.newHashSet("multi-3.7-SNAPSHOT.pom");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("mavenJib", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            assertEquals(7, buildInfo.getModules().size());

            // Assert Maven modules
            Module module = getAndAssertModule(buildInfo, "org.jfrog.test:multi:3.7-SNAPSHOT");
            assertModuleArtifacts(module, expectedArtifacts);
            assertTrue(CollectionUtils.isEmpty(module.getDependencies()));
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi1:3.7-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi2:3.7-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi3:3.7-SNAPSHOT");

            // Assert Docker modules
            module = getAndAssertModule(buildInfo, "multi1");
            assertTrue(CollectionUtils.isNotEmpty(module.getArtifacts()));
            assertDockerModuleProperties(module);
            module = getAndAssertModule(buildInfo, "multi2");
            assertTrue(CollectionUtils.isNotEmpty(module.getArtifacts()));
            assertDockerModuleProperties(module);
            module = getAndAssertModule(buildInfo, "multi3");
            assertTrue(CollectionUtils.isNotEmpty(module.getArtifacts()));
            assertDockerModuleProperties(module);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void gradleTest(String buildName) throws Exception {
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("gradle", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            assertEquals(4, buildInfo.getModules().size());

            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:services:1.0-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:api:1.0-SNAPSHOT");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:shared:1.0-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:webservice:1.0-SNAPSHOT");
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void gradleCiServerTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet(pipelineType.toString() + "-gradle-example-ci-server-1.0.jar", "ivy-1.0.xml", pipelineType.toString() + "-gradle-example-ci-server-1.0.pom");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("gradleCiServer", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertEquals(5, buildInfo.getModules().size());

            Module module = getAndAssertModule(buildInfo, "org.jfrog.example.gradle:" + pipelineType.toString() + "-gradle-example-ci-server:1.0");
            assertModuleArtifacts(module, expectedArtifacts);
            assertTrue(CollectionUtils.isEmpty(module.getDependencies()));

            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:services:1.0");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:api:1.0");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:shared:1.0");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:webservice:1.0");
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void gradleCiServerPublicationTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet(pipelineType.toString() + "-gradle-example-ci-server-publication-1.0.jar", pipelineType.toString() + "-gradle-example-ci-server-publication-1.0.pom");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("gradleCiServerPublication", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertEquals(5, buildInfo.getModules().size());

            Module module = getAndAssertModule(buildInfo, "org.jfrog.example.gradle:" + pipelineType.toString() + "-gradle-example-ci-server-publication:1.0");
            // Gradle 6 and above produce an extra artifact of type "module".
            // In order to allow the test to run on Gradle 6 and above, we remove it.
            module.setArtifacts(module.getArtifacts().stream().filter(art -> !art.getType().equalsIgnoreCase("module")).collect(Collectors.toList()));
            assertModuleArtifacts(module, expectedArtifacts);
            assertTrue(CollectionUtils.isEmpty(module.getDependencies()));

            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:services:1.0");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:api:1.0");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:shared:1.0");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:webservice:1.0");
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void npmTest(String pipelineName, String buildName, String moduleName) throws Exception {
        Set<String> expectedArtifact = Sets.newHashSet("package-name1:0.0.1");
        Set<String> expectedDependencies = Sets.newHashSet("big-integer:1.6.40", "is-number:7.0.0");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineName, false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            Module module = getAndAssertModule(buildInfo, moduleName);
            assertModuleDependencies(module, expectedDependencies);
            assertModuleArtifacts(module, expectedArtifact);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void goTest(String pipelineName, String buildName, String moduleName) throws Exception {
        Set<String> expectedArtifact = Sets.newHashSet("github.com/you/hello:v1.0.0.zip", "github.com/you/hello:v1.0.0.mod", "github.com/you/hello:v1.0.0.info");
        Set<String> expectedDependencies = Sets.newHashSet("rsc.io/sampler:v1.3.0", "golang.org/x/text:v0.0.0-20170915032832-14c0d48ead0c", "rsc.io/quote:v1.5.2");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineName, false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            // TODO - uncomment after making the Go step running in a new Java process:
            // assertFilteredProperties(buildInfo);
            Module module = getAndAssertModule(buildInfo, moduleName);
            assertModuleDependencies(module, expectedDependencies);
            assertModuleArtifacts(module, expectedArtifact);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void conanTest(String pipelineName, String buildName) throws Exception {
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineName, false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            Module module = getAndAssertModule(buildInfo, "DownloadOnly");
            assertTrue(module.getDependencies().size() > 0);
            module = getAndAssertModule(buildInfo, "zlib/1.2.11@conan/stable");
            assertTrue(module.getArtifacts().size() > 0);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void pipTest(String pipelineName, String buildName, String moduleName) throws Exception {
        int expectedDependencies = 5;
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineName, false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            Module module = getAndAssertModule(buildInfo, moduleName);
            assertEquals(expectedDependencies, module.getDependencies().size());
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void nugetTest(String pipelineName, String buildName, String moduleName) throws Exception {
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineName, false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            Module module = getAndAssertModule(buildInfo, moduleName);
            assertTrue(module.getDependencies() != null && module.getDependencies().size() > 0);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void dotnetTest(String pipelineName, String buildName, String moduleName) throws Exception {
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineName, false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            Module module = getAndAssertModule(buildInfo, moduleName);
            assertTrue(module.getDependencies() != null && module.getDependencies().size() > 0);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    @Test
    public void uploadFailNoOpTest() throws Exception {
        try {
            runPipeline("uploadFailNoOp", false);
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains("Fail-no-op: No files were affected in the upload process."));
        }
        for (String fileName : Arrays.asList("a.in", "b.in", "c.in")) {
            assertFalse(isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), fileName));
        }
    }

    @Test
    public void downloadFailNoOpTest() throws Exception {
        try {
            runPipeline("downloadFailNoOp", false);
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains(FAIL_NO_OP_ERROR_MESSAGE));
        }
    }

    void setPropsTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a.in");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("setProps", false);
            // Only a.in is expected to exist in workspace
            assertTrue("a.in doesn't exist locally", isExistInWorkspace(slave, pipelineResults, "setProps-test", "a.in"));
            assertFalse("b.in exists locally", isExistInWorkspace(slave, pipelineResults, "setProps-test", "b.in"));
            assertFalse("c.in exists locally", isExistInWorkspace(slave, pipelineResults, "setProps-test", "c.in"));

            // Make sure all files still exist in artifactory:
            Arrays.asList("a.in", "b.in", "c.in").forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));

            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void deletePropsTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("b.in", "c.in");
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("deleteProps", false);
            // Only b.in, c.in are expected to exist in workspace
            assertFalse("a.in exists locally", isExistInWorkspace(slave, pipelineResults, "deleteProps-test", "a.in"));
            for (String fileName : expectedDependencies) {
                assertTrue(fileName + "doesn't exists locally", isExistInWorkspace(slave, pipelineResults, "deleteProps-test", fileName));
            }

            // Make sure all files exist in artifactory:
            getTestFilesNamesByLayer(0).forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));

            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void dockerPushTest(String buildName) throws Exception {
        Assume.assumeFalse("Skipping Docker tests", SystemUtils.IS_OS_WINDOWS);
        WorkflowRun pipelineResults = null;

        try {
            // Get image name
            String domainName = System.getenv("JENKINS_ARTIFACTORY_DOCKER_PUSH_DOMAIN");
            if (StringUtils.isBlank(domainName)) {
                throw new MissingArgumentException("The JENKINS_ARTIFACTORY_DOCKER_PUSH_DOMAIN environment variable is not set.");
            }
            if (!StringUtils.endsWith(domainName, "/")) {
                domainName += "/";
            }
            String imageName = domainName + "jfrog_artifactory_jenkins_tests:2";
            String host = System.getenv("JENKINS_ARTIFACTORY_DOCKER_HOST");
            DockerJavaWrapper.buildImage(imageName, host, new EnvVars(), getProjectPath("docker-example"));
            // Run pipeline
            pipelineResults = runPipeline("dockerPush", false);

            // Get build info
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertFilteredProperties(buildInfo);
            assertEquals(1, buildInfo.getModules().size());
            List<Module> modules = buildInfo.getModules();
            Module module = modules.get(0);
            assertEquals(7, module.getArtifacts().size());
            assertEquals(5, module.getDependencies().size());

            // Verify image's id exists in build-info.
            List<Artifact> deps = module.getArtifacts();
            assertFalse(deps.isEmpty());
            String imageId = getImageId(imageName, host, null).replace(":", "__");
            assertTrue(deps.stream().anyMatch(dep -> dep.getName().equals(imageId)));
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void dockerPullTest(String buildName) throws Exception {
        WorkflowRun pipelineResults = null;
        try {
            Assume.assumeFalse("Skipping Docker tests", SystemUtils.IS_OS_WINDOWS);
            // Assert 'JENKINS_ARTIFACTORY_DOCKER_PULL_DOMAIN' environment variable exist
            String domainName = System.getenv("JENKINS_ARTIFACTORY_DOCKER_PULL_DOMAIN");
            if (StringUtils.isBlank(domainName)) {
                throw new MissingArgumentException("The JENKINS_ARTIFACTORY_DOCKER_PULL_DOMAIN environment variable is not set.");
            }
            domainName = StringUtils.appendIfMissing(domainName, "/");
            String imageName = domainName + "hello-world:latest";
            String host = System.getenv("JENKINS_ARTIFACTORY_DOCKER_HOST");
            // Run pipeline
            pipelineResults = runPipeline("dockerPull", false);

            // Check that the actual image exist
            assertNotEquals(StringUtils.EMPTY, DockerJavaWrapper.getImageIdFromTag(imageName, host, new EnvVars(), null));

            // Get build info
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            assertEquals(1, buildInfo.getModules().size());
            List<Module> modules = buildInfo.getModules();
            Module module = modules.get(0);
            assertNull(module.getArtifacts());

            // Verify image's id exists in build-info.
            List<Dependency> deps = module.getDependencies();
            assertFalse(deps.isEmpty());
            String imageId = getImageId(imageName, host, null).replace(":", "__");
            assertTrue(deps.stream().anyMatch(dep -> dep.getId().equals(imageId)));
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void xrayScanTest(String buildName, boolean failBuild, boolean printTable) throws Exception {
        Assume.assumeTrue("Skipping Xray tests", JENKINS_XRAY_TEST_ENABLE == null || Boolean.parseBoolean(JENKINS_XRAY_TEST_ENABLE));
        String failStr = String.valueOf(failBuild);
        xrayScanTest(buildName, "xrayScanFailBuild" + StringUtils.capitalize(failStr), failBuild, printTable);
    }

    private void xrayScanTest(String buildName, String pipelineJobName, boolean failBuild, boolean printTable) throws Exception {
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline(pipelineJobName, false);
            if (failBuild) {
                fail("Job expected to fail");
            }
        } catch (AssertionError t) {
            String expecting = "Violations were found by Xray:";
            assertTrue("Expecting message to include: " + expecting + ". Found: " + t.getMessage(),
                    t.getMessage().contains(expecting));

            expecting = "Build " + pipelineType.toString() + ":" + pipelineJobName
                    + " test number 3 was scanned by Xray and 1 Alerts were generated";
            assertTrue("Expecting message to include: " + expecting + ". Found: " + t.getMessage(),
                    t.getMessage().contains(expecting));
        } finally {
            String expecting = new BuildScanTableHelper().TABLE_HEADLINE;
            assertEquals(printTable, pipelineResults.getLog().contains(expecting));
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void collectIssuesTest(String buildName) throws Exception {
        File collectIssuesExample = new File(getIntegrationDir().toFile(), "collectIssues-example");
        File dotGitPath = testTemporaryFolder.newFolder(".git");
        // Copy the provided folder to .git in the tmp folder
        FileUtils.copyDirectory(new File(collectIssuesExample, "buildaddgit_.git_suffix"), dotGitPath);

        // Clear older build if exists
        deleteBuild(artifactoryClient, buildName);
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("collectIssues", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            // Assert Issues
            assertNotNull(buildInfo.getIssues());
            assertNotNull(buildInfo.getIssues().getAffectedIssues());
            assertEquals(4, buildInfo.getIssues().getAffectedIssues().size());
            // Assert Vcs
            assertTrue(CollectionUtils.isNotEmpty(buildInfo.getVcs()));
            assertEquals("b033a0e508bdb52eee25654c9e12db33ff01b8ff", buildInfo.getVcs().get(0).getRevision());
            assertEquals("https://github.com/jfrog/jfrog-cli-go.git", buildInfo.getVcs().get(0).getUrl());
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
        }
    }

    void appendBuildInfoTest(String buildName) throws Exception {
        File collectIssuesExample = new File(getIntegrationDir().toFile(), "collectIssues-example");
        File dotGitPath = testTemporaryFolder.newFolder(".git");
        // Copy the provided folder to .git in the tmp folder
        FileUtils.copyDirectory(new File(collectIssuesExample, "buildaddgit_.git_suffix"), dotGitPath);

        Set<String> expectedArtifacts = getTestFilesNamesByLayer(0);
        // Clear older build if exists
        deleteBuild(artifactoryClient, buildName);
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("append", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName, BUILD_NUMBER, null);
            // Assert Issues
            assertNotNull(buildInfo.getIssues());
            assertNotNull(buildInfo.getIssues().getAffectedIssues());
            assertEquals(4, buildInfo.getIssues().getAffectedIssues().size());
            // Assert Vcs
            assertTrue(CollectionUtils.isNotEmpty(buildInfo.getVcs()));
            assertEquals("b033a0e508bdb52eee25654c9e12db33ff01b8ff", buildInfo.getVcs().get(0).getRevision());
            assertEquals("https://github.com/jfrog/jfrog-cli-go.git", buildInfo.getVcs().get(0).getUrl());
            // Assert artifacts
            expectedArtifacts.forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));
            Module module = getAndAssertModule(buildInfo, "buildInfo tmp");
            assertModuleArtifacts(module, expectedArtifacts);
        } finally {
            cleanupBuilds(pipelineResults, buildName, null, BUILD_NUMBER);
            FileUtils.deleteDirectory(dotGitPath);
        }
    }

    void jfPipelinesOutputResourcesTest() throws Exception {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(1080)) {
            runPipeline("jfPipelinesResources", true);
            HttpRequest[] requests = mockServer.retrieveRecordedRequests(null);
            assertEquals(2, ArrayUtils.getLength(requests));

            for (HttpRequest request : requests) {
                JsonBody body = (JsonBody) request.getBody();
                JsonNode requestTree = createMapper().readTree(body.getValue());
                getAndAssertChild(requestTree, "action", "status");
                getAndAssertChild(requestTree, "stepId", "5");
                String status = getAndAssertChild(requestTree, "status", null).asText();
                if (JFrogPipelinesServer.BUILD_STARTED.equals(status)) {
                    // Check job started
                    checkJenkinsJobInfo(requestTree, false);
                    assertFalse(requestTree.has("outputResources"));
                } else if (Result.SUCCESS.toString().equals(status)) {
                    // Check job completed
                    checkJenkinsJobInfo(requestTree, true);
                    JsonNode outputResources = getAndAssertChild(requestTree, "outputResources", null);
                    assertEquals(2, outputResources.size());
                    for (JsonNode resource : outputResources) {
                        JsonNode name = getAndAssertChild(resource, "name", null);
                        switch (name.asText()) {
                            case "resource1":
                                JsonNode content = getAndAssertChild(resource, "content", null);
                                getAndAssertChild(content, "a", "b");
                                break;
                            case "resource2":
                                content = getAndAssertChild(resource, "content", null);
                                getAndAssertChild(content, "c", "d");
                                break;
                            default:
                                Assert.fail("Unexpected output resource name " + name.asText());
                        }
                    }
                } else {
                    Assert.fail("Unexpected build status " + status);
                }
            }
        }
    }

    public void jfPipelinesReportStatusTest() throws Exception {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(1080)) {
            runPipeline("jfPipelinesReport", true);
            // Get sent request from the mock server
            HttpRequest[] requests = mockServer.retrieveRecordedRequests(null);
            assertEquals(2, ArrayUtils.getLength(requests));

            // Check requests
            for (HttpRequest request : requests) {
                JsonBody body = (JsonBody) request.getBody();
                JsonNode requestTree = createMapper().readTree(body.getValue());
                String status = getAndAssertChild(requestTree, "status", null).asText();
                assertTrue(Result.UNSTABLE.toString().equals(status) || "STARTED".equals(status));
                getAndAssertChild(requestTree, "action", "status");
                getAndAssertChild(requestTree, "stepId", "5");
                checkJenkinsJobInfo(requestTree, false);
                assertFalse(requestTree.has("outputResources"));
            }
        }
    }

    public void buildTriggerGlobalServerTest() throws Exception {
        // Run pipeline
        WorkflowRun run = runPipeline("buildTriggerGlobalServer", false);

        // Check trigger
        ArtifactoryTrigger artifactoryTrigger = checkArtifactoryTrigger(run);

        // Change something in Artifactory server
        ArtifactoryServer server = RepositoriesUtils.getArtifactoryServer("LOCAL");
        assertNotNull(server);
        server.setConnectionRetry(4);

        // Make sure the change took place
        server = artifactoryTrigger.getArtifactoryServer();
        assertNotNull(server);
        assertEquals(4, server.getConnectionRetry());
    }

    public void buildTriggerNewServerTest() throws Exception {
        // Run pipeline
        WorkflowRun run = runPipeline("buildTriggerNewServer", false);

        // Check trigger
        checkArtifactoryTrigger(run);
    }

    void buildAppendTest(String buildName) throws Exception {
        String buildName1 = buildName + "-1";
        String buildNumber1 = BUILD_NUMBER;
        String buildName2 = buildName + "-2";
        String buildNumber2 = BUILD_NUMBER + "-2";
        // Clear older builds if exist
        deleteBuild(artifactoryClient, buildName1);
        deleteBuild(artifactoryClient, buildName2);
        WorkflowRun pipelineResults = null;
        try {
            pipelineResults = runPipeline("buildAppend", false);
            Build buildInfo = artifactoryManager.getBuildInfo(buildName2, buildNumber2, null);
            getAndAssertModule(buildInfo, buildName1 + "/" + buildNumber1);
        } finally {
            cleanupBuilds(pipelineResults, buildName1, null, buildNumber1);
            cleanupBuilds(pipelineResults, buildName2, null, buildNumber2);
        }
    }

    void rbCreateUpdateSign(String releaseBundleName) throws Exception {
        String releaseBundleVersion = "1";
        runPipeline("rbCreateUpdateSign", false);

        GetReleaseBundleStatusResponse status = distributionManager.getReleaseBundleStatus(releaseBundleName, releaseBundleVersion);
        distributionManager.deleteLocalReleaseBundle(releaseBundleName, "1");

        // Make sure release bundle updated
        assertEquals("Update a release bundle", status.getDescription());
        // Make sure release bundle is signed
        assertThat(status.getState(), isOneOf(GetReleaseBundleStatusResponse.DistributionState.SIGNED, GetReleaseBundleStatusResponse.DistributionState.READY_FOR_DISTRIBUTION));
    }

    void rbCreateDistDel(String releaseBundleName) throws Exception {
        String releaseBundleVersion = "1";
        try {
            runPipeline("rbCreateDistDel", false);
            GetReleaseBundleStatusResponse status = distributionManager.getReleaseBundleStatus(releaseBundleName, releaseBundleVersion);
            assertNull(status);
        } finally {
            DeleteReleaseBundleRequest request = new DeleteReleaseBundleRequest() {{
                setOnSuccess(DeleteReleaseBundleRequest.OnSuccess.delete);
                setDistributionRules(Utils.createDistributionRules(new ArrayList<>(), "*", "*"));
            }};
            try {
                distributionManager.deleteReleaseBundle(releaseBundleName, "1", false, request);
                fail("Pipeline 'rbCreateDistDel' failed to delete release bundle '" + releaseBundleName + "'");
            } catch (IOException ignore) {
                // ignore
            }
        }
    }
}
