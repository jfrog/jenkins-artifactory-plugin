package org.jfrog.hudson.util.converters;

import hudson.util.XStream2;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.ResolverOverrider;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.generic.ArtifactoryGenericConfigurator;
import org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator;
import org.jfrog.hudson.ivy.ArtifactoryIvyConfigurator;
import org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator;
import org.jfrog.hudson.maven3.ArtifactoryMaven3NativeConfigurator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author yahavi
 **/
public class DeployerResolverOverriderConverterTest {

    private final ServerDetails noOverrideDetails = new ServerDetails("should-not-override", "", null, null, null, null, null, null);
    private final ServerDetails overrideDetails = new ServerDetails("should-override", "", null, null, null, null, null, null);
    private final DeployerResolverOverriderConverter<ResolverOverrider> genericConverter = new GenericDeployerResolverOverriderConverter<>(new XStream2());
    private final DeployerResolverOverriderConverter<DeployerOverrider> deployerConverter = new DeployerResolverOverriderConverter<>(new XStream2());
    private final DeployerResolverOverriderConverter<ResolverOverrider> resolverConverter = new DeployerResolverOverriderConverter<>(new XStream2());

    @Test
    public void testConvertFromDetails() {
        ArtifactoryGenericConfigurator genericConfigurator = new ArtifactoryGenericConfigurator(overrideDetails, null, null, null, null, null, null);
        genericConverter.callback(genericConfigurator, null);
        assertEquals("should-override", genericConfigurator.getSpecsResolverDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getLegacyResolverDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getSpecsDeployerDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getLegacyDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertFromDetailsNoOverride() {
        ArtifactoryGenericConfigurator genericConfigurator = new ArtifactoryGenericConfigurator(overrideDetails, null, null, noOverrideDetails, noOverrideDetails, noOverrideDetails, noOverrideDetails);
        genericConverter.callback(genericConfigurator, null);
        assertEquals("should-not-override", genericConfigurator.getSpecsResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", genericConfigurator.getLegacyResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", genericConfigurator.getSpecsDeployerDetails().getArtifactoryName());
        assertEquals("should-not-override", genericConfigurator.getLegacyDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertFromDeployerDetails() {
        ArtifactoryGenericConfigurator genericConfigurator = new ArtifactoryGenericConfigurator(null, overrideDetails, null, null, null, null, null);
        genericConverter.callback(genericConfigurator, null);
        assertEquals("should-override", genericConfigurator.getSpecsDeployerDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getLegacyDeployerDetails().getArtifactoryName());
        assertNull(genericConfigurator.getSpecsResolverDetails());
        assertNull(genericConfigurator.getLegacyResolverDetails());
    }

    @Test
    public void testConvertFromResolverDetails() {
        ArtifactoryGenericConfigurator genericConfigurator = new ArtifactoryGenericConfigurator(null, null, overrideDetails, null, null, null, null);
        genericConverter.callback(genericConfigurator, null);
        assertEquals("should-override", genericConfigurator.getSpecsResolverDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getLegacyResolverDetails().getArtifactoryName());
        assertNull(genericConfigurator.getSpecsDeployerDetails());
        assertNull(genericConfigurator.getLegacyDeployerDetails());
    }

    @Test
    public void testConvertFromDeployerResolverDetails() {
        ArtifactoryGenericConfigurator genericConfigurator = new ArtifactoryGenericConfigurator(null, overrideDetails, overrideDetails, null, null, null, null);
        genericConverter.callback(genericConfigurator, null);
        assertEquals("should-override", genericConfigurator.getSpecsResolverDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getLegacyResolverDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getSpecsResolverDetails().getArtifactoryName());
        assertEquals("should-override", genericConfigurator.getLegacyResolverDetails().getArtifactoryName());
    }

    @Test
    public void testConvertFromDeployerResolverDetailsNoOverride() {
        ArtifactoryGenericConfigurator genericConfigurator = new ArtifactoryGenericConfigurator(null, overrideDetails, overrideDetails, noOverrideDetails, noOverrideDetails, noOverrideDetails, noOverrideDetails);
        genericConverter.callback(genericConfigurator, null);
        assertEquals("should-not-override", genericConfigurator.getSpecsResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", genericConfigurator.getLegacyResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", genericConfigurator.getSpecsResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", genericConfigurator.getLegacyResolverDetails().getArtifactoryName());
    }

    @Test
    public void testConvertMavenFromDetails() {
        ArtifactoryMaven3Configurator configurator = new ArtifactoryMaven3Configurator(overrideDetails, null, null);
        resolverConverter.callback(configurator, null);
        assertEquals("should-override", configurator.getResolverDetails().getArtifactoryName());
        assertEquals("should-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertMavenFromDetailsNoOverride() {
        ArtifactoryMaven3Configurator configurator = new ArtifactoryMaven3Configurator(overrideDetails, noOverrideDetails, noOverrideDetails);
        resolverConverter.callback(configurator, null);
        assertEquals("should-not-override", configurator.getResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertMavenNativeFromDetails() {
        ArtifactoryMaven3NativeConfigurator configurator = new ArtifactoryMaven3NativeConfigurator(overrideDetails, null);
        resolverConverter.callback(configurator, null);
        assertEquals("should-override", configurator.getResolverDetails().getArtifactoryName());
        assertEquals("should-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertMavenNativeFromDetailsNoOverride() {
        ArtifactoryMaven3NativeConfigurator configurator = new ArtifactoryMaven3NativeConfigurator(overrideDetails, noOverrideDetails);
        resolverConverter.callback(configurator, null);
        assertEquals("should-not-override", configurator.getResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertGradleFromDetails() {
        ArtifactoryGradleConfigurator configurator = new ArtifactoryGradleConfigurator(overrideDetails, null, null);
        resolverConverter.callback(configurator, null);
        assertEquals("should-override", configurator.getResolverDetails().getArtifactoryName());
        assertEquals("should-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertGradleFromDetailsNoOverride() {
        ArtifactoryGradleConfigurator configurator = new ArtifactoryGradleConfigurator(overrideDetails, noOverrideDetails, noOverrideDetails);
        resolverConverter.callback(configurator, null);
        assertEquals("should-not-override", configurator.getResolverDetails().getArtifactoryName());
        assertEquals("should-not-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertIvyFromDetails() {
        ArtifactoryIvyConfigurator configurator = new ArtifactoryIvyConfigurator(overrideDetails, null);
        deployerConverter.callback(configurator, null);
        assertEquals("should-override", configurator.getDeployerDetails().getArtifactoryName());
        assertEquals("should-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertIvyFromDetailsNoOverride() {
        ArtifactoryIvyConfigurator configurator = new ArtifactoryIvyConfigurator(overrideDetails, noOverrideDetails);
        deployerConverter.callback(configurator, null);
        assertEquals("should-not-override", configurator.getDeployerDetails().getArtifactoryName());
        assertEquals("should-not-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertRedeployPublisherFromDetails() {
        ArtifactoryRedeployPublisher configurator = new ArtifactoryRedeployPublisher(overrideDetails, null);
        deployerConverter.callback(configurator, null);
        assertEquals("should-override", configurator.getDeployerDetails().getArtifactoryName());
        assertEquals("should-override", configurator.getDeployerDetails().getArtifactoryName());
    }

    @Test
    public void testConvertRedeployPublisherFromDetailsNoOverride() {
        ArtifactoryRedeployPublisher configurator = new ArtifactoryRedeployPublisher(overrideDetails, noOverrideDetails);
        deployerConverter.callback(configurator, null);
        assertEquals("should-not-override", configurator.getDeployerDetails().getArtifactoryName());
        assertEquals("should-not-override", configurator.getDeployerDetails().getArtifactoryName());
    }
}
