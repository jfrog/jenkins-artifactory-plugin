package org.jfrog.hudson.pipeline.integration;

import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
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
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static org.jfrog.hudson.pipeline.integration.ITestUtils.*;
import static org.junit.Assert.fail;

/**
 * @author yahavi
 */
public class PipelineTestBase {

    @ClassRule // The Jenkins instance
    public static JenkinsRule jenkins = new JenkinsRule();

    private static final String ARTIFACTORY_URL = System.getenv("JENKINS_ARTIFACTORY_URL");
    private static final String ARTIFACTORY_USERNAME = System.getenv("JENKINS_ARTIFACTORY_USERNAME");
    private static final String ARTIFACTORY_PASSWORD = System.getenv("JENKINS_ARTIFACTORY_PASSWORD");
    static final Path FILES_PATH = getIntegrationDir().resolve("files").toAbsolutePath();

    private static long currentTime = System.currentTimeMillis();
    private static StrSubstitutor pipelineSubstitution;
    static ArtifactoryBuildInfoClient buildInfoClient;
    static Artifactory artifactoryClient;

    private ClassLoader classLoader = PipelineTestBase.class.getClassLoader();
    PipelineType pipelineType;

    PipelineTestBase(PipelineType pipelineType) {
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

    @After
    public void deleteRepos() {
        Arrays.stream(TestRepository.values()).forEach(repoName -> artifactoryClient.repository(getRepoKey(repoName)).delete());
    }

    @AfterClass
    public static void tearDown() {
        buildInfoClient.close();
        artifactoryClient.close();
    }

    /**
     * Get the repository key of the temporary test repository.
     *
     * @param repository - The repository base name
     * @return repository key of the temporary test repository
     */
    static String getRepoKey(TestRepository repository) {
        return String.format("%s-%d", repository.getRepoName(), currentTime);
    }

    /**
     * Create a temporary repository for the tests.
     *
     * @param repository - The repository base name
     */
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

    /**
     * Creates build-info and Artifactory Java clients.
     */
    private static void createClients() {
        buildInfoClient = new ArtifactoryBuildInfoClient(ARTIFACTORY_URL, ARTIFACTORY_USERNAME, ARTIFACTORY_PASSWORD, new NullLog());
        artifactoryClient = ArtifactoryClientBuilder.create()
                .setUrl(ARTIFACTORY_URL)
                .setUsername(ARTIFACTORY_USERNAME)
                .setPassword(ARTIFACTORY_PASSWORD)
                .build();
    }

    /**
     * Creates string substitution for the pipelines. The tests use it to replace strings in the pipelines after
     * loading them.
     */
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

    /**
     * Get the specific test source files dir: jenkins-artifactory-plugin/src/test/resources/integration/projectName.
     *
     * @param projectName - The project name - 'maven-example', 'gradle-example', etc.
     * @return the specific test source files dir
     */
    private static String getProjectPath(String projectName) {
        Path projectPath = getIntegrationDir().resolve(projectName).toAbsolutePath();
        return fixWindowsPath(projectPath.toString());
    }

    /**
     * Run pipeline script.
     *
     * @param name - Pipeline name from 'jenkins-artifactory-plugin/src/test/resources/integration/pipelines'.
     * @return the Jenkins job
     */
    WorkflowRun runPipeline(String name) throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        jenkins.getInstance().getWorkspaceFor(project).mkdirs();
        project.setDefinition(new CpsFlowDefinition(readPipeline(name)));
        return jenkins.buildAndAssertSuccess(project);
    }

    /**
     * Read pipeline from 'jenkins-artifactory-plugin/src/test/resources/integration/pipelines'
     *
     * @param name - The pipeline name
     * @return pipeline as a string
     */
    private String readPipeline(String name) throws IOException {
        String pipelinePath = Paths.get("integration", "pipelines", pipelineType.toString(), name + ".pipeline").toString();
        InputStream inputStream = classLoader.getResourceAsStream(pipelinePath);
        if (inputStream == null) {
            throw new IOException(pipelinePath + " not found");
        }
        String pipeline = IOUtils.toString(inputStream);
        return pipelineSubstitution.replace(pipeline);
    }

    /**
     * Verify ARTIFACTORY_URL, ARTIFACTORY_USERNAME and ARTIFACTORY_PASSWORD
     */
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

    /**
     * Set ARTIFACTORY_JARS_LIB env to be used in Maven and Gradle tests.
     * The Maven and Gradle steps will copy the jars from this directory to the local test cache.
     */
    private static void setJarsLibEnv() {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("ARTIFACTORY_JARS_LIB", Paths.get("target", "artifactory", "WEB-INF", "lib").toAbsolutePath().toString());
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }
}
