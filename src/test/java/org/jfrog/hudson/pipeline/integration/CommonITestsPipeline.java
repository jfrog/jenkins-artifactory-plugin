package org.jfrog.hudson.pipeline.integration;

import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.junit.Test;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;

import static org.jfrog.hudson.pipeline.integration.ITestUtils.*;
import static org.junit.Assert.*;

/**
 * @author yahavi
 */
public class CommonITestsPipeline extends PipelineTestBase {

    CommonITestsPipeline(PipelineType pipelineType) {
        super(pipelineType);
    }

    void uploadTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet("a.in", "b.in", "c.in");
        String buildNumber = "3";

        runPipeline("upload");
        try {
            Arrays.asList("a.in", "b.in", "c.in").forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleArtifacts(module, expectedArtifacts);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void downloadTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a.in", "b.in", "c.in");
        String buildNumber = "3";

        Files.list(FILES_PATH).forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        WorkflowRun build = runPipeline("download");
        try {
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(jenkins, build, "download-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void promotionTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a.in", "b.in", "c.in");
        String buildNumber = "4";

        Files.list(FILES_PATH).forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        WorkflowRun build = runPipeline("promote");
        try {
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(jenkins, build, "promotion-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
            // In this tests, the expected dependencies and artifacts are equal
            assertModuleArtifacts(module, expectedDependencies);
            assertNoArtifactsInRepo(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1));
            assertArtifactsInRepo(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO2), expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void mavenTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet("multi-3.7-SNAPSHOT.pom");
        String buildNumber = "3";
        try {
            runPipeline("maven");
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            assertEquals(4, buildInfo.getModules().size());

            Module module = getAndAssertModule(buildInfo, "org.jfrog.test:multi:3.7-SNAPSHOT");
            assertModuleArtifacts(module, expectedArtifacts);
            assertTrue(CollectionUtils.isEmpty(module.getDependencies()));
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi1:3.7-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi2:3.7-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.test:multi3:3.7-SNAPSHOT");
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void gradleTest(String buildName) throws Exception {
        String buildNumber = "3";
        try {
            runPipeline("gradle");
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            assertEquals(4, buildInfo.getModules().size());

            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:services:1.0-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:api:1.0-SNAPSHOT");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:shared:1.0-SNAPSHOT");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:webservice:1.0-SNAPSHOT");
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void gradleCiServerTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet(pipelineType.toString() + "-gradle-example-ci-server-1.0.jar", "ivy-1.0.xml", pipelineType.toString() + "-gradle-example-ci-server-1.0.pom");
        String buildNumber = "3";
        try {
            runPipeline("gradleCiServer");
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            assertEquals(5, buildInfo.getModules().size());

            Module module = getAndAssertModule(buildInfo, "org.jfrog.example.gradle:" + pipelineType.toString() + "-gradle-example-ci-server:1.0");
            assertModuleArtifacts(module, expectedArtifacts);
            assertTrue(CollectionUtils.isEmpty(module.getDependencies()));

            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:services:1.0");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:api:1.0");
            assertModuleContainsArtifacts(buildInfo, "org.jfrog.example.gradle:shared:1.0");
            assertModuleContainsArtifactsAndDependencies(buildInfo, "org.jfrog.example.gradle:webservice:1.0");
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void npmTest(String buildName) throws Exception {
        Set<String> expectedArtifact = Sets.newHashSet("package-name1:0.0.1");
        Set<String> expectedDependencies = Sets.newHashSet("big-integer-1.6.40.tgz", "is-number-7.0.0.tgz");
        String buildNumber = "3";
        try {
            runPipeline("npm");
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, "package-name1:0.0.1");
            assertModuleDependencies(module, expectedDependencies);
            assertModuleArtifacts(module, expectedArtifact);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    @Test
    public void uploadFailNoOpTest() throws Exception {
        try {
            runPipeline("uploadFailNoOp");
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
            runPipeline("downloadFailNoOp");
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains("Fail-no-op: No files were affected in the download process."));
        }
    }
}
