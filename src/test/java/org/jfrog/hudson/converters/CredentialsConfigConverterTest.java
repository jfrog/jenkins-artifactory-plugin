package org.jfrog.hudson.converters;

import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class CredentialsConfigConverterTest {
    
    private static final String DEPLOYER_USERNAME = "deployer";
    private static final String DEPLOYER_PASSWORD = "$ecretDeployer";
    private static final String RESOLVER_USERNAME = "resolver";
    private static final String RESOLVER_PASSWORD = "$ecretResolver";
    
    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();
    
    @Test
    public void testConvertToSecret_pre_3_6_0() throws IOException {
        ArtifactoryBuilder.DescriptorImpl testBuilder = ExtensionList.lookupSingleton(ArtifactoryBuilder.DescriptorImpl.class);
        
        File testFile = new File(getClass().getResource("/converters/config-3.5.0.xml").getFile());
        Files.copy(testFile.toPath(), new File(Jenkins.get().getRootDir(), ArtifactoryBuilder.class.getName() + ".xml").toPath());
        testBuilder.load();
        Assert.assertThat(testBuilder.getArtifactoryServers(), Matchers.hasSize(1));
        ArtifactoryServer artifactoryServer = testBuilder.getArtifactoryServers().get(0);
        Assert.assertEquals(DEPLOYER_USERNAME, Objects.requireNonNull(artifactoryServer.getDeployerCredentialsConfig().getUsername()));
        Assert.assertEquals(DEPLOYER_PASSWORD, Objects.requireNonNull(artifactoryServer.getDeployerCredentialsConfig().getPassword()).getPlainText());
        Assert.assertEquals(RESOLVER_USERNAME, Objects.requireNonNull(artifactoryServer.getResolverCredentialsConfig().getUsername()));
        Assert.assertEquals(RESOLVER_PASSWORD, Objects.requireNonNull(artifactoryServer.getResolverCredentialsConfig().getPassword()).getPlainText());
    }
}
