package org.jfrog.hudson.pipeline;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.model.Repository;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.platform.commons.util.StringUtils;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class PipelineITestBase {

    private static final String LOCAL_REPO = "jenkins-tests-repo";
    private static final String ARTIFACTORY_URL = System.getenv("JENKINS_ARTIFACTORY_URL");
    private static final String ARTIFACTORY_USERNAME = System.getenv("JENKINS_ARTIFACTORY_USERNAME");
    private static final String ARTIFACTORY_PASSWORD = System.getenv("JENKINS_ARTIFACTORY_PASSWORD");

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private ClassLoader classLoader = getClass().getClassLoader();
    private Path filesDir = Paths.get("src", "test", "resources", "files").toAbsolutePath();
    private static String localRepo;
    private static Artifactory artifactoryClient;

    @BeforeClass
    public static void setUp() {
        verifyEnvironment();
        localRepo = LOCAL_REPO + "-" + System.currentTimeMillis();
        artifactoryClient = ArtifactoryClientBuilder.create()
                .setUrl(ARTIFACTORY_URL)
                .setUsername(ARTIFACTORY_USERNAME)
                .setPassword(ARTIFACTORY_PASSWORD)
                .build();

        Repository repository = artifactoryClient.repositories()
                .builders()
                .localRepositoryBuilder()
                .key(localRepo)
                .description("Jenkins Artifactory tests repository")
                .build();

        artifactoryClient.repositories().create(1, repository);
    }

    @AfterClass
    public static void tearDown() {
        if (artifactoryClient != null) {
            artifactoryClient.repository(localRepo).delete();
        }
    }

    String readPipeline(String name) throws IOException {
        InputStream inputStream = classLoader.getResourceAsStream(Paths.get("pipelines", "scripted", name).toString());
        if (inputStream == null) {
            throw new IOException(name + " not found");
        }
        String pipeline = IOUtils.toString(inputStream);


        Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("FILES_DIR", filesDir.resolve("*").toString());
        valuesMap.put("LOCAL_REPO", localRepo);

        StrSubstitutor strSubstitutor = new StrSubstitutor(valuesMap);
        return strSubstitutor.replace(pipeline);
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
