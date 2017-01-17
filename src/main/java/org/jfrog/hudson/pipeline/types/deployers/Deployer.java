package org.jfrog.hudson.pipeline.types.deployers;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.Filter;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.Env;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.publisher.PublisherContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tamirh on 04/08/2016.
 */
public abstract class Deployer implements DeployerOverrider, Serializable {
    private boolean deployArtifacts = true;
    private boolean includeEnvVars;
    private List<String> properties = new ArrayList<String>();
    private Filter artifactDeploymentPatterns = new Filter();
    private String customBuildName = "";

    protected ArtifactoryServer server;

    public boolean isIncludeEnvVars() {
        return includeEnvVars;
    }

    // Shouldn't be whitelisted, the includeEnvVars value is been taken from the buildInfo configurations.
    public Deployer setIncludeEnvVars(boolean includeEnvVars) {
        this.includeEnvVars = includeEnvVars;
        return this;
    }

    public org.jfrog.hudson.ArtifactoryServer getArtifactoryServer() {
        return Utils.prepareArtifactoryServer(null, this.server);
    }

    @Whitelisted
    public ArtifactoryServer getServer() {
        return server;
    }

    @Whitelisted
    public Deployer setServer(ArtifactoryServer server) {
        this.server = server;
        return this;
    }

    @Whitelisted
    public boolean isDeployArtifacts() {
        return deployArtifacts;
    }

    @Whitelisted
    public Deployer setDeployArtifacts(boolean deployArtifacts) {
        this.deployArtifacts = deployArtifacts;
        return this;
    }

    @Whitelisted
    public Deployer addProperty(String key, String... values) {
        StringBuilder prop = new StringBuilder(key + "=");
        for (int i = 0; i < values.length; i++) {
            prop.append(values[i]);
            if (i != values.length - 1) {
                prop.append(",");
            }
        }
        properties.add(prop.toString());
        return this;
    }

    protected String buildPropertiesString() {
        StringBuilder props = new StringBuilder();

        for (int i = 0; i < properties.size(); i++) {
            props.append(properties.get(i));
            if (i != properties.size() - 1) {
                props.append(";");
            }
        }
        return props.toString();
    }

    public boolean isOverridingDefaultDeployer() {
        return false;
    }

    public Credentials getOverridingDeployerCredentials() {
        return null;
    }

    public CredentialsConfig getDeployerCredentialsConfig() {
        try {
            return getArtifactoryServer().getDeployerCredentialsConfig();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Artifactory server is missing.");
        }
    }

    public boolean isDeployBuildInfo() {
        // By default we don't want to deploy buildInfo when we are running pipeline flow
        return false;
    }

    @Whitelisted
    public Filter getArtifactDeploymentPatterns() {
        return artifactDeploymentPatterns;
    }

    public IncludesExcludes getArtifactsIncludeExcludeForDeyployment() {
        return Utils.getArtifactsIncludeExcludeForDeyployment(artifactDeploymentPatterns.getPatternFilter());
    }

    public void createPublisherBuildInfoDetails(BuildInfo buildInfo) {
        if (buildInfo != null) {
            Env buildInfoEnv = buildInfo.getEnv();
            this.setIncludeEnvVars(buildInfoEnv.isCapture());
            this.setCustomBuildName(buildInfo.getName());
        }
    }

    public String getCustomBuildName() {
        return customBuildName;
    }

    public void setCustomBuildName(String customBuildName) {
        this.customBuildName = customBuildName;
    }

    public abstract ServerDetails getDetails();
    public abstract PublisherContext.Builder getContextBuilder();
    public abstract boolean isEmpty();
}
