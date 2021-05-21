package org.jfrog.hudson.util.converters;

import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ArtifactoryBuilderConverterTest {

    private static final String SERVER_ID = "1";
    private static final String PLATFORM_URL = "https://jfrog.io";
    private static final String ARTIFACTORY_URL = "https://jfrog.io/artifactory";

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testConvertToArtifactoryServers_pre_3_10_6() throws IOException {
        ArtifactoryBuilder.DescriptorImpl testBuilder = ExtensionList.lookupSingleton(ArtifactoryBuilder.DescriptorImpl.class);
        File testFile = new File(getClass().getResource("/converters/config-3.10.6-1.xml").getFile());
        Path targetPath = new File(Jenkins.get().getRootDir(), ArtifactoryBuilder.class.getName() + ".xml").toPath();
        Files.copy(testFile.toPath(), targetPath);
        testBuilder.load();
        Assert.assertThat(testBuilder.getJfrogInstances(), Matchers.hasSize(2));
        JFrogPlatformInstance jfrogServer = testBuilder.getJfrogInstances().get(0);
        Assert.assertEquals(SERVER_ID, Objects.requireNonNull(jfrogServer.getId()));
        Assert.assertEquals(SERVER_ID, Objects.requireNonNull(jfrogServer.getArtifactory().getServerId()));
        Assert.assertEquals(ARTIFACTORY_URL, Objects.requireNonNull(jfrogServer.getArtifactory().getArtifactoryUrl()));
        Assert.assertNull(jfrogServer.getUrl());
        Files.delete(targetPath);
    }

    @Test
    public void testConvertToArtifactoryServers_after_3_10_6() throws IOException {
        ArtifactoryBuilder.DescriptorImpl testBuilder = ExtensionList.lookupSingleton(ArtifactoryBuilder.DescriptorImpl.class);
        File testFile = new File(getClass().getResource("/converters/config-3.10.6-3.xml").getFile());
        Path targetPath = new File(Jenkins.get().getRootDir(), ArtifactoryBuilder.class.getName() + ".xml").toPath();
        Files.copy(testFile.toPath(), targetPath);
        testBuilder.load();
        Assert.assertThat(testBuilder.getJfrogInstances(), Matchers.hasSize(1));
        JFrogPlatformInstance jfrogServer = testBuilder.getJfrogInstances().get(0);
        Assert.assertEquals(SERVER_ID, Objects.requireNonNull(jfrogServer.getId()));
        Assert.assertEquals(SERVER_ID, Objects.requireNonNull(jfrogServer.getArtifactory().getServerId()));
        Assert.assertEquals(ARTIFACTORY_URL, Objects.requireNonNull(jfrogServer.getArtifactory().getArtifactoryUrl()));
        Assert.assertEquals(PLATFORM_URL, Objects.requireNonNull(jfrogServer.getUrl()));
        Files.delete(targetPath);
    }
}
