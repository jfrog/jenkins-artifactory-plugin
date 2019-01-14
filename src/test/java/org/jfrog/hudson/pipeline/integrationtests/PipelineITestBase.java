package org.jfrog.hudson.pipeline.integrationtests;

import com.google.common.collect.Sets;
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.jfrog.hudson.pipeline.integrationtests.ITestUtils.*;
import static org.junit.Assert.*;

public class PipelineITestBase {

    @ClassRule // The Jenkins instance
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public Timeout globalTimeout = new Timeout(10, TimeUnit.MINUTES);

    private static final String ARTIFACTORY_URL = System.getenv("JENKINS_ARTIFACTORY_URL");
    private static final String ARTIFACTORY_USERNAME = System.getenv("JENKINS_ARTIFACTORY_USERNAME");
    private static final String ARTIFACTORY_PASSWORD = System.getenv("JENKINS_ARTIFACTORY_PASSWORD");
    private static final Path FILES_PATH = getIntegrationDir().resolve("files").toAbsolutePath();

    private static long currentTime = System.currentTimeMillis();
    private static StrSubstitutor pipelineSubstitution;
    private static ArtifactoryBuildInfoClient buildInfoClient;
    private static Artifactory artifactoryClient;

    private ClassLoader classLoader = PipelineITestBase.class.getClassLoader();
    private PipelineType pipelineType;

    PipelineITestBase(PipelineType pipelineType) {
        this.pipelineType = pipelineType;
    }

    @BeforeClass
    public static void setUp() {
        verifyEnvironment();
        setJarsLibEnv();
        createClients();
        cleanUpArtifactory(artifactoryClient);
        createPipelineSubstitution();
    }

    @Before
    public void createRepos() {
        Arrays.stream(TestRepository.values()).forEach(this::createRepo);
    }

    private static String getRepoKey(TestRepository repository) {
        return String.format("%s-%d", repository.getRepoName(), currentTime);
    }

    private void createRepo(TestRepository repository) {
        try {
            String repositorySettingsPath = Paths.get("integration", "settings", repository.getRepoName() + ".json").toString();
            InputStream inputStream = classLoader.getResourceAsStream(repositorySettingsPath);
            if (inputStream == null) {
                throw new IOException(repositorySettingsPath + " not found");
            }
            String repositorySettings = IOUtils.toString(inputStream, "UTF-8");
            artifactoryClient.restCall(new ArtifactoryRequestImpl()
                    .method(ArtifactoryRequest.Method.PUT)
                    .requestType(ArtifactoryRequest.ContentType.JSON)
                    .apiUrl("api/repositories/" + getRepoKey(repository))
                    .requestBody(repositorySettings));
        } catch (Exception e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @After
    public void deleteRepos() {
        Arrays.stream(TestRepository.values()).forEach(repoName -> artifactoryClient.repository(getRepoKey(repoName)).delete());
    }

    @AfterClass
    public static void tearDown() {
        buildInfoClient.close();
        artifactoryClient.close();
    }

    private static void createClients() {
        buildInfoClient = new ArtifactoryBuildInfoClient(ARTIFACTORY_URL, ARTIFACTORY_USERNAME, ARTIFACTORY_PASSWORD, new NullLog());
        artifactoryClient = ArtifactoryClientBuilder.create()
                .setUrl(ARTIFACTORY_URL)
                .setUsername(ARTIFACTORY_USERNAME)
                .setPassword(ARTIFACTORY_PASSWORD)
                .build();
    }

    private static void createPipelineSubstitution() {
        pipelineSubstitution = new StrSubstitutor(new HashMap<String, String>() {{
            put("FILES_DIR", fixWindowsPath(FILES_PATH.toString() + File.separator + "*"));
            put("MAVEN_PROJECT_PATH", getProjectPath("maven-example"));
            put("GRADLE_PROJECT_PATH", getProjectPath("gradle-example"));
            put("GRADLE_CI_PROJECT_PATH", getProjectPath("gradle-example-ci"));
            put("NPM_PROJECT_PATH", getProjectPath("npm-example"));
            put("LOCAL_REPO1", getRepoKey(TestRepository.LOCAL_REPO1));
            put("LOCAL_REPO2", getRepoKey(TestRepository.LOCAL_REPO2));
            put("JCENTER_REMOTE_REPO", getRepoKey(TestRepository.JCENTER_REMOTE_REPO));
            put("NPM_LOCAL", getRepoKey(TestRepository.NPM_LOCAL));
            put("NPM_REMOTE", getRepoKey(TestRepository.NPM_REMOTE));
        }});
    }

    private static String getProjectPath(String projectName) {
        Path projectPath = getIntegrationDir().resolve(projectName).toAbsolutePath();
        return fixWindowsPath(projectPath.toString());
    }

    private WorkflowRun buildWorkflowProject(String name) throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        jenkins.getInstance().getWorkspaceFor(project).mkdirs(); // TODO - Delete after fix
        project.setDefinition(new CpsFlowDefinition(readPipeline(name)));
        return jenkins.buildAndAssertSuccess(project);
    }

    private String readPipeline(String name) throws IOException {
        String pipelinePath = Paths.get("integration", "pipelines", pipelineType.toString(), name + ".pipeline").toString();
        InputStream inputStream = classLoader.getResourceAsStream(pipelinePath);
        if (inputStream == null) {
            throw new IOException(pipelinePath + " not found");
        }
        String pipeline = IOUtils.toString(inputStream);
        return pipelineSubstitution.replace(pipeline);
    }

    private static void verifyEnvironment() {
        if (StringUtils.isBlank(ARTIFACTORY_URL)) {
            throw new IllegalArgumentException("JENKINS_ARTIFACTORY_URL is not set");
        }
        if (StringUtils.isBlank(ARTIFACTORY_USERNAME)) {
            throw new IllegalArgumentException("JENKINS_ARTIFACTORY_USERNAME is not set");
        }
        if (StringUtils.isBlank(ARTIFACTORY_PASSWORD)) {
            throw new IllegalArgumentException("JENKINS_ARTIFACTORY_PASSWORD is not set");
        }
    }

    private static void setJarsLibEnv() {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("ARTIFACTORY_JARS_LIB", Paths.get("target", "artifactory", "WEB-INF", "lib").toAbsolutePath().toString());
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

    void uploadTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet("a.in", "b.in", "c.in");
        String buildNumber = "3";

        buildWorkflowProject("upload");
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
        WorkflowRun build = buildWorkflowProject("download");
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
        WorkflowRun build = buildWorkflowProject("promote");
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
            buildWorkflowProject("maven");
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
            buildWorkflowProject("gradle");
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
        Set<String> expectedArtifacts = Sets.newHashSet("gradle-example-ci-server-1.0.jar", "ivy-1.0.xml", "gradle-example-ci-server-1.0.pom");
        String buildNumber = "3";
        try {
            buildWorkflowProject("gradleCiServer");
            Build buildInfo = getBuildInfo(buildInfoClient, buildName, buildNumber);
            assertEquals(5, buildInfo.getModules().size());

            Module module = getAndAssertModule(buildInfo, "org.jfrog.example.gradle:gradle-example-ci-server:1.0");
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
            buildWorkflowProject("npm");
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
            buildWorkflowProject("uploadFailNoOp");
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
            buildWorkflowProject("downloadFailNoOp");
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains("Fail-no-op: No files were affected in the download process."));
        }
    }

}
