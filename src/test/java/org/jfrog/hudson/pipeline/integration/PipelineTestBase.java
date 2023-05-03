package org.jfrog.hudson.pipeline.integration;

import hudson.FilePath;
import hudson.model.Label;
import hudson.model.Slave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.access.AccessManager;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.DistributionManager;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.jfrog.hudson.jfpipelines.JFrogPipelinesServer;
import org.jfrog.hudson.jfpipelines.Utils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.jfrog.hudson.pipeline.integration.ITestUtils.*;
import static org.junit.Assert.fail;

/**
 * @author yahavi
 */
public class PipelineTestBase {

    @ClassRule // The Jenkins instance
    public static JenkinsRule jenkins = new JenkinsRule();
    static Slave slave;
    private static final Logger log = Logger.getLogger(PipelineTestBase.class.getName());
    @Rule
    public TestName testName = new TestName();
    @ClassRule
    public static TemporaryFolder testTemporaryFolder = new TemporaryFolder();

    private static final Path INTEGRATION_BASE_PATH = Paths.get(".").toAbsolutePath().normalize()
            .resolve(Paths.get("src", "test", "resources", "integration"));
    private static final String SLAVE_LABEL = "TestSlave";
    private static final String PLATFORM_URL = System.getenv("JENKINS_PLATFORM_URL");
    private static final String ARTIFACTORY_URL = StringUtils.removeEnd(PLATFORM_URL, "/") + "/artifactory";
    private static final String DISTRIBUTION_URL = StringUtils.removeEnd(PLATFORM_URL, "/") + "/distribution";
    private static final String ACCESS_URL = StringUtils.removeEnd(PLATFORM_URL, "/") + "/access";
    private static final String ARTIFACTORY_USERNAME = System.getenv("JENKINS_PLATFORM_USERNAME");
    private static final String ACCESS_TOKEN = System.getenv("JENKINS_PLATFORM_ADMIN_TOKEN");
    static final String JENKINS_XRAY_TEST_ENABLE = System.getenv("JENKINS_XRAY_TEST_ENABLE");
    static final String JENKINS_DOCKER_TEST_DISABLE = System.getenv("JENKINS_DOCKER_TEST_DISABLE");
    static final Path FILES_PATH = getIntegrationDir().resolve("files").toAbsolutePath();
    public static final String BUILD_NUMBER = System.getProperty("os.name") + System.currentTimeMillis();
    public static final String PROJECT_KEY = "j" + StringUtils.right(String.valueOf(System.currentTimeMillis()), 5);
    public static final String PROJECT_CONFIGURATION_FILE_NAME = "jenkins-artifactory-tests-project-conf";

    private static long currentTime;
    private static StringSubstitutor pipelineSubstitution;
    static ArtifactoryManager artifactoryManager;
    static DistributionManager distributionManager;
    static AccessManager accessManager;
    static Artifactory artifactoryClient;

    private static final ClassLoader classLoader = PipelineTestBase.class.getClassLoader();
    PipelineType pipelineType;

    PipelineTestBase(PipelineType pipelineType) {
        this.pipelineType = pipelineType;
    }

    @BeforeClass
    public static void setUp() {
        currentTime = System.currentTimeMillis();
        verifyEnvironment();
        createSlave();
        setEnvVars();
        createClients();
        setGlobalConfiguration();
        cleanUpArtifactory(artifactoryClient);
        // Create repositories
        createPipelineSubstitution();
        Arrays.stream(TestRepository.values()).forEach(PipelineTestBase::createRepo);
        createProject();
    }

    @Before
    public void beforeTest() throws IOException {
        log.info("Running test: " + pipelineType + " / " + testName.getMethodName());
        FileUtils.cleanDirectory(testTemporaryFolder.getRoot().getAbsoluteFile());
    }

    @After
    public void afterTests() {
        // Remove the content of all local repositories
        Arrays.stream(TestRepository.values()).filter(repository -> repository.getRepoType() == TestRepository.RepoType.LOCAL)
                .forEach(repository -> artifactoryClient.repository(getRepoKey(repository)).delete(StringUtils.EMPTY));
    }

    @AfterClass
    public static void tearDown() {
        // Remove repositories - need to remove virtual repositories first
        Stream.concat(
                        Arrays.stream(TestRepository.values()).filter(repository -> repository.getRepoType() == TestRepository.RepoType.VIRTUAL),
                        Arrays.stream(TestRepository.values()).filter(repository -> repository.getRepoType() != TestRepository.RepoType.VIRTUAL))
                .forEach(repository -> artifactoryClient.repository(getRepoKey(repository)).delete());
        // Remove project.
        try {
            accessManager.deleteProject(PROJECT_KEY);
        } catch (Exception e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
        artifactoryManager.close();
        distributionManager.close();
        artifactoryClient.close();
        accessManager.close();
    }

    /**
     * Create jenkins slave. All tests should run on it.
     */
    private static void createSlave() {
        try {
            slave = jenkins.createOnlineSlave(Label.get(SLAVE_LABEL));
        } catch (Exception e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
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
    private static void createRepo(TestRepository repository) {
        try {
            String repositorySettings = readConfigurationWithSubstitution(repository.getRepoName());
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
     * Read repository or project configuration and replace placeholders with their corresponding values.
     *
     * @param repoOrProject - Name of configuration in resources.
     * @return The configuration after substitution.
     */
    private static String readConfigurationWithSubstitution(String repoOrProject) {
        try {
            String repositorySettings = FileUtils.readFileToString(INTEGRATION_BASE_PATH
                    .resolve("settings")
                    .resolve(repoOrProject + ".json").toFile(), StandardCharsets.UTF_8);
            return pipelineSubstitution.replace(repositorySettings);
        } catch (Exception e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    /**
     * Creates a project in platform.
     */
    private static void createProject() {
        try {
            String projectConf = readConfigurationWithSubstitution(PROJECT_CONFIGURATION_FILE_NAME);
            accessManager.createProject(projectConf);
        } catch (Exception e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Creates build-info and Artifactory Java clients.
     */
    private static void createClients() {
        artifactoryManager = new ArtifactoryManager(ARTIFACTORY_URL, ACCESS_TOKEN, new NullLog());
        distributionManager = new DistributionManager(DISTRIBUTION_URL, ACCESS_TOKEN, new NullLog());
        artifactoryClient = ArtifactoryClientBuilder.create()
                .setUrl(ARTIFACTORY_URL)
                .setAccessToken(ACCESS_TOKEN)
                .build();
        accessManager = new AccessManager(ACCESS_URL, ACCESS_TOKEN, new NullLog());
    }

    /**
     * For jfPipelines tests - Create JFrog Pipelines server in the Global configuration.
     * For buildTrigger tests - Create an empty list of Artifactory servers.
     */
    private static void setGlobalConfiguration() {
        ArtifactoryBuilder.DescriptorImpl artifactoryBuilder = (ArtifactoryBuilder.DescriptorImpl) jenkins.getInstance().getDescriptor(ArtifactoryBuilder.class);
        Assert.assertNotNull(artifactoryBuilder);
        JFrogPipelinesServer server = new JFrogPipelinesServer("http://127.0.0.1:1080", CredentialsConfig.EMPTY_CREDENTIALS_CONFIG, 300, false, 3);
        artifactoryBuilder.setJfrogPipelinesServer(server);
        CredentialsConfig cred = new CredentialsConfig("admin", "password", "cred1");
        CredentialsConfig platformCred = new CredentialsConfig(ARTIFACTORY_USERNAME, ACCESS_TOKEN, null);
        List<JFrogPlatformInstance> artifactoryServers = new ArrayList<JFrogPlatformInstance>() {{
            add(new JFrogPlatformInstance(new ArtifactoryServer("LOCAL", "http://127.0.0.1:8081/artifactory", cred, cred, 0, false, 3, null)));
            add(new JFrogPlatformInstance("PLATFORM", PLATFORM_URL, ARTIFACTORY_URL, DISTRIBUTION_URL, platformCred, platformCred, 0, false, 3, null));
        }};
        artifactoryBuilder.setJfrogInstances(artifactoryServers);
    }

    /**
     * Creates string substitution for the pipelines. The tests use it to replace strings in the pipelines after
     * loading them.
     */
    private static void createPipelineSubstitution() {
        pipelineSubstitution = new StringSubstitutor(new HashMap<String, String>() {{
            put("FILES_DIR", fixWindowsPath(FILES_PATH + File.separator + "*"));
            put("FILES_DIR_1", fixWindowsPath(FILES_PATH + File.separator + "1" + File.separator + "*"));
            put("MAVEN_PROJECT_PATH", getProjectPath("maven-example"));
            put("MAVEN_JIB_PROJECT_PATH", getProjectPath("maven-jib-example"));
            put("GRADLE_PROJECT_PATH", getProjectPath("gradle-example"));
            put("GRADLE_CI_PROJECT_PATH", getProjectPath("gradle-example-ci"));
            put("GRADLE_CI_PUBLICATION_PROJECT_PATH", getProjectPath("gradle-example-ci-publications"));
            put("NPM_PROJECT_PATH", getProjectPath("npm-example"));
            put("GO_PROJECT_PATH", getProjectPath("go-example"));
            put("PIP_PROJECT_PATH", getProjectPath("pip-example"));
            put("CONAN_PROJECT_PATH", getProjectPath("conan-example"));
            put("DOCKER_PROJECT_PATH", getProjectPath("docker-example"));
            put("NUGET_PROJECT_PATH", getProjectPath("nuget-example"));
            put("DOTNET_PROJECT_PATH", getProjectPath("dotnet-example"));
            put("TEST_TEMP_FOLDER", fixWindowsPath(testTemporaryFolder.getRoot().getAbsolutePath()));
            put("LOCAL_REPO1", getRepoKey(TestRepository.LOCAL_REPO1));
            put("LOCAL_REPO2", getRepoKey(TestRepository.LOCAL_REPO2));
            put("JCENTER_REMOTE_REPO", getRepoKey(TestRepository.JCENTER_REMOTE_REPO));
            put("NPM_LOCAL", getRepoKey(TestRepository.NPM_LOCAL));
            put("NPM_REMOTE", getRepoKey(TestRepository.NPM_REMOTE));
            put("GO_LOCAL", getRepoKey(TestRepository.GO_LOCAL));
            put("GO_REMOTE", getRepoKey(TestRepository.GO_REMOTE));
            put("GO_VIRTUAL", getRepoKey(TestRepository.GO_VIRTUAL));
            put("PIP_REMOTE", getRepoKey(TestRepository.PIP_REMOTE));
            put("PIP_VIRTUAL", getRepoKey(TestRepository.PIP_VIRTUAL));
            put("CONAN_LOCAL", getRepoKey(TestRepository.CONAN_LOCAL));
            put("NUGET_REMOTE", getRepoKey(TestRepository.NUGET_REMOTE));
            put("BUILD_NUMBER", BUILD_NUMBER);
            put("PROJECT_KEY", PROJECT_KEY);
        }});
    }

    /**
     * Get the specific test source files dir: jenkins-artifactory-plugin/src/test/resources/integration/projectName.
     *
     * @param projectName - The project name - 'maven-example', 'gradle-example', etc.
     * @return the specific test source files dir
     */
    static String getProjectPath(String projectName) {
        Path projectPath = getIntegrationDir().resolve(projectName).toAbsolutePath();
        return fixWindowsPath(projectPath.toString());
    }

    /**
     * Run pipeline script.
     *
     * @param name                     - Pipeline name from 'jenkins-artifactory-plugin/src/test/resources/integration/pipelines'
     * @param injectPipelinesParameter - True if this is a JFrog pipelines test
     * @return the Jenkins job
     */
    WorkflowRun runPipeline(String name, boolean injectPipelinesParameter) throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        if (injectPipelinesParameter) {
            Utils.injectJfPipelinesInfoParameter(project, "{\"stepId\":\"5\"}"); // For JFrog Pipelines tests
        }
        FilePath slaveWs = slave.getWorkspaceFor(project);
        if (slaveWs == null) {
            throw new Exception("Slave workspace not found");
        }
        slaveWs.mkdirs();
        project.setDefinition(new CpsFlowDefinition(readPipeline(name), false));
        return jenkins.buildAndAssertSuccess(project);
    }

    /**
     * Read pipeline from 'jenkins-artifactory-plugin/src/test/resources/integration/pipelines'
     *
     * @param name - The pipeline name
     * @return pipeline as a string
     */
    private String readPipeline(String name) throws IOException {
        String pipeline = FileUtils.readFileToString(INTEGRATION_BASE_PATH
                .resolve("pipelines")
                .resolve(pipelineType.toString())
                .resolve(name + ".pipeline").toFile(), StandardCharsets.UTF_8);
        return pipelineSubstitution.replace(pipeline);
    }

    /**
     * Verify ARTIFACTORY_URL, ARTIFACTORY_USERNAME and ACCESS_TOKEN
     */
    private static void verifyEnvironment() {
        if (StringUtils.isBlank(PLATFORM_URL)) {
            throw new IllegalArgumentException("JENKINS_PLATFORM_URL is not set");
        }
        if (StringUtils.isBlank(ARTIFACTORY_USERNAME)) {
            throw new IllegalArgumentException("JENKINS_PLATFORM_USERNAME is not set");
        }
        if (StringUtils.isBlank(ACCESS_TOKEN)) {
            throw new IllegalArgumentException("JENKINS_PLATFORM_ADMIN_TOKEN is not set");
        }
    }

    /**
     * Set node environment variables.
     */
    private static void setEnvVars() {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty(
                // Set ARTIFACTORY_JARS_LIB env to be used in Maven and Gradle tests.
                // The Maven and Gradle steps will copy the jars from this directory to the local test cache.
                new EnvironmentVariablesNodeProperty.Entry("ARTIFACTORY_JARS_LIB",
                        Paths.get("target", "artifactory", "WEB-INF", "lib").toAbsolutePath().toString())
        );
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
    }

    /**
     * Returns a set of the files names in a layer of the FILES_PATH directory.
     * Base = layer 0.
     */
    Set<String> getTestFilesNamesByLayer(int layer) {
        Set<String> names = new HashSet<>();
        String pathStr = FILES_PATH.toString();
        pathStr += layer == 0 ? "" : File.separator + layer;

        File folder = new File(pathStr);
        File[] listOfFiles = folder.listFiles();
        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                names.add(file.getName());
            }
        }
        return names;
    }
}