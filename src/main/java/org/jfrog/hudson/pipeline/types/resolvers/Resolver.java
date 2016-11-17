package org.jfrog.hudson.pipeline.types.resolvers;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.ResolverOverrider;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.util.Credentials;

import java.io.Serializable;

/**
 * Created by Tamirh on 16/08/2016.
 */
public abstract class Resolver implements ResolverOverrider, Serializable {

    protected ArtifactoryServer server;

    public org.jfrog.hudson.ArtifactoryServer getArtifactoryServer() {
        return Utils.prepareArtifactoryServer(null, this.server);
    }

    @Whitelisted
    public ArtifactoryServer getServer() {
        return server;
    }

    @Whitelisted
    public void setServer(ArtifactoryServer server) {
        this.server = server;
    }

    public boolean isOverridingDefaultResolver() {
        return false;
    }

    public Credentials getOverridingResolverCredentials() {
        return null;
    }

    public CredentialsConfig getResolverCredentialsConfig() {
        try {
            return getArtifactoryServer().getDeployerCredentialsConfig();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Artifactory server is missing.");
        }
    }

    public abstract ServerDetails getResolverDetails();
    public abstract boolean isEmpty();
}
