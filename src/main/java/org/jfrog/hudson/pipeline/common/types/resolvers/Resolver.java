package org.jfrog.hudson.pipeline.common.types.resolvers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.ResolverOverrider;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
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

    @JsonIgnore
    public CredentialsConfig getResolverCredentialsConfig() {
        try {
            return getArtifactoryServer().getDeployerCredentialsConfig();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Artifactory server is missing.");
        }
    }

    @JsonIgnore
    public abstract ServerDetails getResolverDetails();
    public abstract boolean isEmpty();
}
