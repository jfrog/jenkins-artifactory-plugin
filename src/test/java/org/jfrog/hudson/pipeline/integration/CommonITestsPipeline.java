package org.jfrog.hudson.pipeline.integration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.collect.Sets;
import hudson.EnvVars;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.hudson.pipeline.common.docker.utils.DockerUtils;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

    void downloadByPatternTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        String buildNumber = "3";

        Files.list(FILES_PATH).filter(Files::isRegularFile)
                .forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        WorkflowRun build = runPipeline("downloadByPattern");
        try {
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, build, "downloadByPattern-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void downloadByAqlTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        String buildNumber = "3";

        Files.list(FILES_PATH).filter(Files::isRegularFile)
                .forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        WorkflowRun build = runPipeline("downloadByAql");
        try {
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, build, "downloadByAql-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void downloadByPatternAndBuildTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a.in");
        String buildNumber = "5";

        Set<String> unexpected = getTestFilesNamesByLayer(0);
        unexpected.addAll(getTestFilesNamesByLayer(1));
        unexpected.removeAll(expectedDependencies);
        WorkflowRun build = runPipeline("downloadByPatternAndBuild");
        try {
            assertTrue(isExistInWorkspace(slave, build, "downloadByPatternAndBuild-test", "a.in"));
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, build, "downloadByPatternAndBuild-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void downloadByBuildOnlyTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        Set<String> unexpected = getTestFilesNamesByLayer(1);
        String buildNumber = "5";

        WorkflowRun build = runPipeline("downloadByBuildOnly");
        try {
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, build, "downloadByBuildOnly-test", fileName));
            }
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, build, "downloadByBuildOnly-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void downloadNonExistingBuildTest(String buildName) throws Exception {
        try {
            runPipeline("downloadNonExistingBuild");
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains("Fail-no-op: No files were affected in the download process."));
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    /**
     * Upload a file to 2 different builds.
     * Verify that we don't download files with same sha and different build name and build number.
     */
    void downloadByShaAndBuildTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a3");
        Set<String> unexpected = Sets.newHashSet("a4", "a5");
        String buildNumber = "6";

        WorkflowRun build = runPipeline("downloadByShaAndBuild");
        try {
            // Only a.in should be in workspace
            assertTrue(isExistInWorkspace(slave, build, "downloadByShaAndBuild-test", "a3"));
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, build, "downloadByShaAndBuild-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
            deleteBuild(artifactoryClient, buildName + "-second");
        }
    }

    /**
     * Upload a file to 2 different builds.
     * Verify that we don't download files with same sha and build name and different build number.
     */
    void downloadByShaAndBuildNameTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a4");
        Set<String> unexpected = Sets.newHashSet("a3", "a5");
        String buildNumber = "6";

        WorkflowRun build = runPipeline("downloadByShaAndBuildName");
        try {
            // Only a.in should be in workspace
            assertTrue(isExistInWorkspace(slave, build, "downloadByShaAndBuildName-test", "a4"));
            for (String fileName : unexpected) {
                assertFalse(isExistInWorkspace(slave, build, "downloadByShaAndBuildName-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
            deleteBuild(artifactoryClient, buildName + "-second");
        }
    }

    void uploadTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = getTestFilesNamesByLayer(0);
        String buildNumber = "3";

        runPipeline("upload");
        try {
            expectedArtifacts.forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleArtifacts(module, expectedArtifacts);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void uploadDownloadCustomModuleNameTest(String buildName) throws Exception {
        Set<String> expectedArtifactsAndDependencies = getTestFilesNamesByLayer(0);
        String buildNumber = "3";

        WorkflowRun build = runPipeline("uploadDownloadCustomModuleName");
        try {
            expectedArtifactsAndDependencies.forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));
            for (String fileName : expectedArtifactsAndDependencies) {
                assertTrue(isExistInWorkspace(slave, build, "downloadByPattern-test", fileName));
            }
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, "my-generic-module");
            assertModuleDependencies(module, expectedArtifactsAndDependencies);
            assertModuleArtifacts(module, expectedArtifactsAndDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void promotionTest(String buildName) throws Exception {
        Set<String> expectedDependencies = getTestFilesNamesByLayer(0);
        String buildNumber = "4";

        Files.list(FILES_PATH).filter(Files::isRegularFile)
                .forEach(file -> uploadFile(artifactoryClient, file, getRepoKey(TestRepository.LOCAL_REPO1)));
        WorkflowRun build = runPipeline("promote");
        try {
            for (String fileName : expectedDependencies) {
                assertTrue(isExistInWorkspace(slave, build, "promotion-test", fileName));
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

    void npmTest(String pipelineName, String buildName, String moduleName) throws Exception {
        Set<String> expectedArtifact = Sets.newHashSet("package-name1:0.0.1");
        Set<String> expectedDependencies = Sets.newHashSet("big-integer-1.6.40.tgz", "is-number-7.0.0.tgz");
        String buildNumber = "3";
        try {
            runPipeline(pipelineName);
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, moduleName);
            assertModuleDependencies(module, expectedDependencies);
            assertModuleArtifacts(module, expectedArtifact);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void goTest(String pipelineName, String buildName, String moduleName) throws Exception {
        Set<String> expectedArtifact = Sets.newHashSet("github.com/you/hello:v1.0.0.zip", "github.com/you/hello:v1.0.0.mod", "github.com/you/hello:v1.0.0.info");
        Set<String> expectedDependencies = Sets.newHashSet("rsc.io/sampler:v1.3.0", "golang.org/x/text:v0.0.0-20170915032832-14c0d48ead0c", "rsc.io/quote:v1.5.2");
        String buildNumber = "7";
        try {
            runPipeline(pipelineName);
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, moduleName);
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

    void setPropsTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("a.in");
        String buildNumber = "3";

        WorkflowRun build = runPipeline("setProps");
        try {
            // Only a.in is expected to exist in workspace
            assertTrue("a.in doesn't exist locally", isExistInWorkspace(slave, build, "setProps-test", "a.in"));
            assertFalse("b.in exists locally", isExistInWorkspace(slave, build, "setProps-test", "b.in"));
            assertFalse("c.in exists locally", isExistInWorkspace(slave, build, "setProps-test", "c.in"));

            // Make sure all files still exist in artifactory:
            Arrays.asList("a.in", "b.in", "c.in").forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));

            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void deletePropsTest(String buildName) throws Exception {
        Set<String> expectedDependencies = Sets.newHashSet("b.in", "c.in");
        String buildNumber = "3";

        WorkflowRun build = runPipeline("deleteProps");
        try {
            // Only b.in, c.in are expected to exist in workspace
            assertFalse("a.in exists locally", isExistInWorkspace(slave, build, "deleteProps-test", "a.in"));
            for (String fileName : expectedDependencies) {
                assertTrue(fileName + "doesn't exists locally", isExistInWorkspace(slave, build, "deleteProps-test", fileName));
            }

            // Make sure all files exist in artifactory:
            getTestFilesNamesByLayer(0).forEach(artifactName ->
                    assertTrue(artifactName + " doesn't exist in Artifactory", isExistInArtifactory(artifactoryClient, getRepoKey(TestRepository.LOCAL_REPO1), artifactName)));

            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            Module module = getAndAssertModule(buildInfo, buildName);
            assertModuleDependencies(module, expectedDependencies);
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void dockerPushTest(String buildName) throws Exception {
        Assume.assumeFalse("Skipping Docker tests", SystemUtils.IS_OS_WINDOWS);
        Assume.assumeTrue("Skipping Xray tests", JENKINS_DOCKER_TEST_ENABLE == null || Boolean.parseBoolean(JENKINS_DOCKER_TEST_ENABLE));
        try {
            // Get image name
            String domainName = System.getenv("JENKINS_ARTIFACTORY_DOCKER_DOMAIN");
            if (StringUtils.isBlank(domainName)) {
                throw new MissingArgumentException("The JENKINS_ARTIFACTORY_DOCKER_DOMAIN environment variable is not set.");
            }
            if (!StringUtils.endsWith(domainName, "/")) {
                domainName += "/";
            }
            String imageName = domainName + "jfrog_artifactory_jenkins_tests:2";
            String host = System.getenv("JENKINS_ARTIFACTORY_DOCKER_HOST");
            DockerClient dockerClient = DockerUtils.getDockerClient(host, new EnvVars());
            String projectPath = getProjectPath("docker-example");
            // Build the docker image with the name provided from env.
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(Paths.get(projectPath).toFile()).withTags(new HashSet<>(Arrays.asList(imageName)));
            buildImageCmd.exec(new BuildImageResultCallback()).awaitImageId();
            // Run pipeline
            runPipeline("dockerPush");
            String buildNumber = "3";

            // Get build info
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            assertEquals(1, buildInfo.getModules().size());
            List<Module> modules = buildInfo.getModules();
            Module module = modules.get(0);
            assertEquals(7, module.getArtifacts().size());
            assertEquals(5, module.getDependencies().size());
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void xrayScanTest(String buildName, boolean failBuild) throws Exception {
        Assume.assumeTrue("Skipping Xray tests", JENKINS_XRAY_TEST_ENABLE == null || Boolean.parseBoolean(JENKINS_XRAY_TEST_ENABLE));
        String str = String.valueOf(failBuild);
        xrayScanTest(buildName, "xrayScanFailBuild" + str.substring(0, 1).toUpperCase() + str.substring(1), failBuild);
    }

    private void xrayScanTest(String buildName, String pipelineJobName, boolean failBuild) throws Exception {
        try {
            runPipeline(pipelineJobName);
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
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void collectIssuesTest(String buildName) throws Exception {
        File collectIssuesExample = new File(getIntegrationDir().toFile(), "collectIssues-example");
        File dotGitPath = testTemporaryFolder.newFolder(".git");
        // Copy the provided folder to .git in the tmp folder
        FileUtils.copyDirectory(new File(collectIssuesExample, "buildaddgit_.git_suffix"), dotGitPath);

        String buildNumber = "3";
        // Clear older build if exists
        deleteBuild(artifactoryClient, buildName);
        runPipeline("collectIssues");
        try {
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            // Assert Issues
            assertNotNull(buildInfo.getIssues());
            assertNotNull(buildInfo.getIssues().getAffectedIssues());
            assertEquals(4, buildInfo.getIssues().getAffectedIssues().size());
            // Assert Vcs
            assertTrue(CollectionUtils.isNotEmpty(buildInfo.getVcs()));
            assertEquals("b033a0e508bdb52eee25654c9e12db33ff01b8ff", buildInfo.getVcs().get(0).getRevision());
            assertEquals("https://github.com/jfrog/jfrog-cli-go.git", buildInfo.getVcs().get(0).getUrl());
        } finally {
            deleteBuild(artifactoryClient, buildName);
        }
    }

    void appendBuildInfoTest(String buildName) throws Exception {
        File collectIssuesExample = new File(getIntegrationDir().toFile(), "collectIssues-example");
        File dotGitPath = testTemporaryFolder.newFolder(".git");
        // Copy the provided folder to .git in the tmp folder
        FileUtils.copyDirectory(new File(collectIssuesExample, "buildaddgit_.git_suffix"), dotGitPath);

        Set<String> expectedArtifacts = getTestFilesNamesByLayer(0);
        String buildNumber = "3";
        // Clear older build if exists
        deleteBuild(artifactoryClient, buildName);
        WorkflowRun build = runPipeline("append");
        try {
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
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
            deleteBuild(artifactoryClient, buildName);
            FileUtils.deleteDirectory(dotGitPath);
        }
    }
}
