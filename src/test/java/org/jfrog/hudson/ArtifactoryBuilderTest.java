package org.jfrog.hudson;

import hudson.ExtensionList;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ArtifactoryBuilderTest {
    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testAutoFillPlatformServers() {
        // Init tests
        ArtifactoryBuilder.DescriptorImpl testBuilder = ExtensionList.lookupSingleton(ArtifactoryBuilder.DescriptorImpl.class);
        JFrogPlatformInstance jfrogPlatformInstance = new JFrogPlatformInstance("testInstance", "www.jfrog.platform.com", "", "", null, null, 0, false, 0, 0);
        testBuilder.setJfrogInstances(Collections.singletonList(jfrogPlatformInstance));

        // Check auto fill on empty servers URL
        testBuilder.autoFillPlatformServers(testBuilder.getJfrogInstances());
        assertEquals(testBuilder.getJfrogInstances().get(0).getPlatformUrl(), "www.jfrog.platform.com");
        assertEquals(testBuilder.getJfrogInstances().get(0).getArtifactoryUrl(), "www.jfrog.platform.com/artifactory");
        assertEquals(testBuilder.getJfrogInstances().get(0).getDistributionUrl(), "www.jfrog.platform.com/distribution");

        // Check auto fill on changing platform URL
        JFrogPlatformInstance newJfrogPlatformInstance = new JFrogPlatformInstance("testInstance", "www.new.jfrog.platform.com", "www.jfrog.platform.com/artifactory", "www.jfrog.platform.com/distribution", null, null, 0, false, 0, 0);

        testBuilder.autoFillPlatformServers(Collections.singletonList(newJfrogPlatformInstance));
        assertEquals("www.new.jfrog.platform.com", newJfrogPlatformInstance.getPlatformUrl());
        assertEquals("www.new.jfrog.platform.com/artifactory", newJfrogPlatformInstance.getArtifactoryUrl());
        assertEquals("www.new.jfrog.platform.com/distribution", newJfrogPlatformInstance.getDistributionUrl());

        // Check auto fill on changing platform URL nad artifactory URL
        newJfrogPlatformInstance = new JFrogPlatformInstance("testInstance", "www.new.jfrog.platform.com", "www.new2.jfrog.platform.com/artifactory", "www.jfrog.platform.com/distribution", null, null, 0, false, 0, 0);
        testBuilder.autoFillPlatformServers(Collections.singletonList(newJfrogPlatformInstance));
        assertEquals("www.new.jfrog.platform.com", newJfrogPlatformInstance.getPlatformUrl());
        assertEquals("www.new2.jfrog.platform.com/artifactory", newJfrogPlatformInstance.getArtifactoryUrl());
        assertEquals("www.new.jfrog.platform.com/distribution", newJfrogPlatformInstance.getDistributionUrl());

        // Check auto fill on changing platform URL and distribution URL
        newJfrogPlatformInstance = new JFrogPlatformInstance("testInstance", "www.new.jfrog.platform.com", "www.jfrog.platform.com/artifactory", "www.new2.jfrog.platform.com/distribution", null, null, 0, false, 0, 0);
        testBuilder.autoFillPlatformServers(Collections.singletonList(newJfrogPlatformInstance));
        assertEquals("www.new.jfrog.platform.com", newJfrogPlatformInstance.getPlatformUrl());
        assertEquals("www.new.jfrog.platform.com/artifactory", newJfrogPlatformInstance.getArtifactoryUrl());
        assertEquals("www.new2.jfrog.platform.com/distribution", newJfrogPlatformInstance.getDistributionUrl());

        // Check auto fill on changing platform ,artifactory and distribution URL
        newJfrogPlatformInstance = new JFrogPlatformInstance("testInstance", "www.new333.jfrog.platform.com", "www.new444.jfrog.platform.com/artifactory", "www.new555.jfrog.platform.com/distribution", null, null, 0, false, 0, 0);
        testBuilder.autoFillPlatformServers(Collections.singletonList(newJfrogPlatformInstance));
        assertEquals("www.new333.jfrog.platform.com", newJfrogPlatformInstance.getPlatformUrl());
        assertEquals("www.new444.jfrog.platform.com/artifactory", newJfrogPlatformInstance.getArtifactoryUrl());
        assertEquals("www.new555.jfrog.platform.com/distribution", newJfrogPlatformInstance.getDistributionUrl());
    }
}
