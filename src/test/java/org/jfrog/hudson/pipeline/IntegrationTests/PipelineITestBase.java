package org.jfrog.hudson.pipeline.IntegrationTests;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.LightweightRepository;
import org.jfrog.artifactory.client.model.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.platform.commons.util.StringUtils;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl.LOCAL;
import static org.jfrog.hudson.pipeline.IntegrationTests.ITestUtils.getResourcesDir;

public class PipelineITestBase {

    // The Jenkins instance
    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private static final String LOCAL_REPO = "jenkins-tests-repo";
    private static final String ARTIFACTORY_URL = System.getenv("JENKINS_ARTIFACTORY_URL");
    private static final String ARTIFACTORY_USERNAME = System.getenv("JENKINS_ARTIFACTORY_USERNAME");
    private static final String ARTIFACTORY_PASSWORD = System.getenv("JENKINS_ARTIFACTORY_PASSWORD");
    static Path FILES_PATH = getResourcesDir().resolve("files").toAbsolutePath();

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
        verifyEnvironment();
        localRepo = LOCAL_REPO + "-" + System.currentTimeMillis();
        artifactoryClient = ArtifactoryClientBuilder.create()
                .setUrl(ARTIFACTORY_URL)
                .setUsername(ARTIFACTORY_USERNAME)
                .setPassword(ARTIFACTORY_PASSWORD)
                .build();
        deleteOldRepos();
        Map<String, String> valuesToSubstitute = new HashMap<String, String>() {{
            put("FILES_DIR", FILES_PATH.toString() + "*");
            put("LOCAL_REPO", localRepo);
        }};
        pipelineSubstitute = new StrSubstitutor(valuesToSubstitute);
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

    private static void deleteOldRepos() {
        artifactoryClient.repositories().list(LOCAL).stream()
                .map(LightweightRepository::getKey)
                .filter(repoKey -> repoKey.startsWith(LOCAL_REPO))
                .forEach(repoKey -> artifactoryClient.repository(repoKey).delete());
    }

    WorkflowRun buildWorkflowProject(String name) throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(readPipeline(name)));
        return jenkins.buildAndAssertSuccess(project);
    }

    private String readPipeline(String name) throws IOException {
        InputStream inputStream = classLoader.getResourceAsStream(Paths.get("pipelines", pipelineType.toString(), name + ".pipeline").toString());
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
}
