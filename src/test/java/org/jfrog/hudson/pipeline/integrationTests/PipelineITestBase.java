package org.jfrog.hudson.pipeline.integrationTests;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.LightweightRepository;
import org.jfrog.artifactory.client.model.Repository;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.junit.*;
import org.junit.platform.commons.util.StringUtils;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl.LOCAL;
import static org.jfrog.hudson.pipeline.integrationTests.ITestUtils.*;
import static org.junit.Assert.*;

public class PipelineITestBase {

    @ClassRule // The Jenkins instance
    public static JenkinsRule jenkins = new JenkinsRule();

    private static final String LOCAL_REPO = "jenkins-tests-repo";
    private static final String ARTIFACTORY_URL = System.getenv("JENKINS_ARTIFACTORY_URL");
    private static final String ARTIFACTORY_USERNAME = System.getenv("JENKINS_ARTIFACTORY_USERNAME");
    private static final String ARTIFACTORY_PASSWORD = System.getenv("JENKINS_ARTIFACTORY_PASSWORD");
    private static final Path MAVEN_PROJECT_PATH = getIntegrationDir().resolve("maven-example").toAbsolutePath();
    private static final Path GRADLE_PROJECT_PATH = getIntegrationDir().resolve("gradle-example").toAbsolutePath();
    static Path FILES_PATH = getIntegrationDir().resolve("files").toAbsolutePath();
    private static ArtifactoryBuildInfoClient buildInfoClient;

    private static ClassLoader classLoader = PipelineITestBase.class.getClassLoader();
    private static StrSubstitutor pipelineSubstitute;
    private PipelineType pipelineType;
    static Artifactory artifactoryClient;
    static String localRepo;

    PipelineITestBase(PipelineType pipelineType) {
        this.pipelineType = pipelineType;
    }

    @BeforeClass
    public static void setUp() {
        localRepo = LOCAL_REPO + "-" + System.currentTimeMillis();
        verifyEnvironment();
        createClients();
        cleanUpRepos();
        createPipelineSubstitute();
    }

    @Before
    public void createRepo() {
        Repository repository = artifactoryClient.repositories()
                .builders()
                .localRepositoryBuilder()
                .key(localRepo)
                .description("Jenkins Artifactory tests repository")
                .build();
        artifactoryClient.repositories().create(1, repository);
    }

    @After
    public void deleteRepo() {
        artifactoryClient.repository(localRepo).delete();
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

    private static void cleanUpRepos() {
        artifactoryClient.repositories().list(LOCAL).stream()
                .map(LightweightRepository::getKey)
                .filter(repoKey -> repoKey.startsWith(LOCAL_REPO))
                .forEach(repoKey -> artifactoryClient.repository(repoKey).delete());
    }

    private static void createPipelineSubstitute() {
        Map<String, String> valuesToSubstitute = new HashMap<String, String>() {{
            put("FILES_DIR", fixWindowsPath(FILES_PATH.toString() + File.separator + "*"));
            put("MAVEN_PROJECT_PATH", fixWindowsPath(MAVEN_PROJECT_PATH.toString()));
            put("GRADLE_PROJECT_PATH", fixWindowsPath(GRADLE_PROJECT_PATH.toString()));
            put("LOCAL_REPO", localRepo);
        }};
        pipelineSubstitute = new StrSubstitutor(valuesToSubstitute);
    }

    WorkflowRun buildWorkflowProject(String name) throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        jenkins.getInstance().getWorkspaceFor(project).mkdirs();
        project.setDefinition(new CpsFlowDefinition(readPipeline(name)));
        return jenkins.buildAndAssertSuccess(project);
    }

    private String readPipeline(String name) throws IOException {
        InputStream inputStream = classLoader.getResourceAsStream(Paths.get("integration", "pipelines", pipelineType.toString(), name + ".pipeline").toString());
        if (inputStream == null) {
            throw new IOException(name + " not found");
        }
        String pipeline = IOUtils.toString(inputStream);
        return pipelineSubstitute.replace(pipeline);
    }

    private static void verifyEnvironment() {
        if (StringUtils.isBlank(ARTIFACTORY_URL)) {
            throw new IllegalArgumentException("JENKINS_ARTIFACTORY_URL is not set");
        }
        if (StringUtils.isBlank(ARTIFACTORY_USERNAME)) {
            throw new IllegalArgumentException("ARTIFACTORY_USERNAME is not set");
        }
        if (StringUtils.isBlank(ARTIFACTORY_PASSWORD)) {
            throw new IllegalArgumentException("ARTIFACTORY_PASSWORD is not set");
        }
    }

    void uploadTest(String buildName) throws Exception {
        Set<String> expectedArtifacts = Sets.newHashSet("a.in", "b.in", "c.in");
        String buildNumber = "3";

        buildWorkflowProject("upload");
        try {
            for (String fileName : Arrays.asList("a.in", "b.in", "c.in")) {
                assertTrue(isExistInArtifactory(artifactoryClient, localRepo, fileName));
            }
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

        Files.list(FILES_PATH).forEach(file -> uploadFile(artifactoryClient, file, localRepo));
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

    @Test
    public void uploadFailNoOpTest() throws Exception {
        try {
            buildWorkflowProject("uploadFailNoOp");
            fail("Job expected to fail");
        } catch (AssertionError t) {
            assertTrue(t.getMessage().contains("Fail-no-op: No files were affected in the upload process."));
        }
        for (String fileName : Arrays.asList("a.in", "b.in", "c.in")) {
            assertFalse(isExistInArtifactory(artifactoryClient, localRepo, fileName));
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

//    @Test
//    public void mavenTest() throws Exception {
//        buildWorkflowProject("maven");
//    }
//
//    @Test
//    public void gradleTest() throws Exception {
//        buildWorkflowProject("gradle");
//    }

}
