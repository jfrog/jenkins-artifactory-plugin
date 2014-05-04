package org.jfrog.hudson.maven3.extractor;

import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.maven3.ArtifactoryMaven3NativeConfigurator;

/**
 * Helper for maven native projects to check if Artifactory is configured in any way (publishing or resolving).
 *
 * @author Shay Yaakov
 */
public abstract class MavenExtractorHelper {

    public static boolean isDisabled(AbstractBuild build) {
        return getPublisherResolverTuple(build) == null;
    }

    public static PublisherResolverTuple getPublisherResolverTuple(AbstractBuild build) {
        if (!(build instanceof MavenModuleSetBuild)) {
            return null;
        }

        MavenModuleSet project = (MavenModuleSet) build.getProject();

        ArtifactoryRedeployPublisher publisher = ActionableHelper.getPublisher(project,
                ArtifactoryRedeployPublisher.class);
        if (publisher != null && !publisher.isApplicable(build)) {
            publisher = null;
        }
        ArtifactoryMaven3NativeConfigurator resolver = ActionableHelper.getBuildWrapper(
                project, ArtifactoryMaven3NativeConfigurator.class);

        if (publisher == null && resolver == null) {
            return null;
        }

        PublisherResolverTuple publisherResolverTuple = new PublisherResolverTuple();
        publisherResolverTuple.publisher = publisher;
        publisherResolverTuple.resolver = resolver;
        return publisherResolverTuple;
    }

    public static class PublisherResolverTuple {
        public ArtifactoryRedeployPublisher publisher;
        public ArtifactoryMaven3NativeConfigurator resolver;
    }
}