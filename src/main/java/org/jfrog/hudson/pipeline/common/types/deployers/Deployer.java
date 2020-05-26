package org.jfrog.hudson.pipeline.common.types.deployers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.DeployerOverrider;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.generic.GenericArtifactsDeployer;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.Filter;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Env;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.ProxyUtils;
import org.jfrog.hudson.util.publisher.PublisherContext;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.jfrog.build.extractor.ModuleParallelDeployHelper.DEFAULT_DEPLOYMENT_THREADS;

/**
 * Created by Tamirh on 04/08/2016.
 */
public abstract class Deployer implements DeployerOverrider, Serializable {
    private boolean deployArtifacts = true;
    private boolean includeEnvVars;
    private ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    private Filter artifactDeploymentPatterns = new Filter();
    private String customBuildName = "";
    private int threads = DEFAULT_DEPLOYMENT_THREADS;
    private transient CpsScript cpsScript;

    protected transient ArtifactoryServer server;

    public boolean isIncludeEnvVars() {
        return includeEnvVars;
    }

    // Shouldn't be whitelisted, the includeEnvVars value is taken from the buildInfo configurations.
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
    public int getThreads() {
        return threads;
    }

    @Whitelisted
    public Deployer setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    @Whitelisted
    public Deployer addProperty(String key, String... values) {
        properties.putAll(key, Arrays.asList(values));
        return this;
    }

    public ArrayListMultimap<String, String> getProperties() {
        return this.properties;
    }

    public void setProperties(ArrayListMultimap<String, String> properties) {
        this.properties = properties;
    }

    public boolean isOverridingDefaultDeployer() {
        return false;
    }

    public Credentials getOverridingDeployerCredentials() {
        return null;
    }

    @JsonIgnore
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

    public void setArtifactDeploymentPatterns(Filter artifactDeploymentPatterns) {
        this.artifactDeploymentPatterns = artifactDeploymentPatterns;
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

    public abstract ServerDetails getDetails() throws IOException;

    public abstract PublisherContext.Builder getContextBuilder() throws IOException;

    public abstract boolean isEmpty();

    public abstract String getTargetRepository(String deployPath);

    public CpsScript getCpsScript() {
        return cpsScript;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    @Whitelisted
    public void deployArtifacts(BuildInfo buildInfo) {
        Map<String, Object> stepVariables = new LinkedHashMap<>();
        stepVariables.put("deployer", this);
        stepVariables.put("buildInfo", buildInfo);
        cpsScript.invokeMethod("deployArtifacts", stepVariables);
    }

    public void deployArtifacts(BuildInfo buildInfo, TaskListener listener, FilePath ws, Run build) throws IOException, InterruptedException {
        if (buildInfo.getDeployableArtifactsByModule().isEmpty()) {
            listener.getLogger().println("No artifacts for deployment were found");
            return;
        }
        String agentName = Utils.getAgentName(ws);
        if (buildInfo.getAgentName().equals(agentName)) {
            org.jfrog.hudson.ArtifactoryServer artifactoryServer = Utils.prepareArtifactoryServer(null, server);
            Credentials credentials = getDeployerCredentialsConfig().provideCredentials(build.getParent());
            if (credentials == Credentials.EMPTY_CREDENTIALS) {
                throw new RuntimeException(String.format(
                        "No matching credentials was found in Jenkins for the supplied credentialsId: '%s' ",
                        getDeployerCredentialsConfig().getCredentialsId()));
            }
            ProxyConfiguration proxy = ProxyUtils.createProxyConfiguration();
            Map<String, Set<DeployDetails>> deployableArtifactsByModule = ws.act(new DeployDetailsCallable(buildInfo.getDeployableArtifactsByModule(), listener, this));
            if (deployableArtifactsByModule == null) {
                throw new RuntimeException("Deployment failed");
            }
            if (!deployableArtifactsByModule.isEmpty()) {
                ws.act(new GenericArtifactsDeployer.FilesDeployerCallable(listener, deployableArtifactsByModule, artifactoryServer, credentials, proxy, getThreads()));
            }
        } else {
            throw new RuntimeException("Cannot deploy the files from agent: " + agentName + " since they were built on agent: " + buildInfo.getAgentName());
        }
    }

    public static class DeployDetailsCallable extends MasterToSlaveFileCallable<Map<String, Set<DeployDetails>>> {
        private static final String SHA1 = "SHA1";
        private static final String MD5 = "MD5";
        private Map<String, List<DeployDetails>> deployableArtifactsPaths;
        private TaskListener listener;
        private Deployer deployer;

        DeployDetailsCallable(Map<String, List<DeployDetails>> deployableArtifactsPaths, TaskListener listener, Deployer deployer) {
            this.deployableArtifactsPaths = deployableArtifactsPaths;
            this.listener = listener;
            this.deployer = deployer;
        }

        public Map<String, Set<DeployDetails>> invoke(File file, VirtualChannel virtualChannel) throws IOException {
            boolean isSuccess = true;
            Map<String, Set<DeployDetails>> results = new LinkedHashMap<>();
            for (Map.Entry<String, List<DeployDetails>> entry : deployableArtifactsPaths.entrySet()) {
                String module = entry.getKey();
                List<DeployDetails> paths = entry.getValue();
                try {
                    Set<DeployDetails> deployDetails = Sets.newLinkedHashSet();
                    for (DeployDetails artifact : paths) {
                        String artifactPath = artifact.getArtifactPath();
                        if (PatternMatcher.pathConflicts(artifactPath, deployer.getArtifactDeploymentPatterns().getPatternFilter())) {
                            listener.getLogger().println("Artifactory Deployer: Skipping the deployment of '" + artifactPath + "' due to the defined include-exclude patterns.");
                            continue;
                        }
                        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifact.getFile(), SHA1, MD5);
                        if (!checksums.get(SHA1).equals(artifact.getSha1())) {
                            listener.error("SHA1 mismatch at '" + artifactPath + "' expected: " + artifact.getSha1() + ", got " + checksums.get(SHA1)
                                    + ". Make sure that the same artifacts were not built more than once.");
                            isSuccess = false;
                        } else {
                            DeployDetails.Builder builder = new DeployDetails.Builder()
                                    .file(artifact.getFile())
                                    .artifactPath(artifactPath)
                                    .targetRepository(deployer.getTargetRepository(artifactPath))
                                    .md5(checksums.get(MD5)).sha1(artifact.getSha1())
                                    .addProperties(artifact.getProperties()).addProperties(deployer.getProperties());
                            deployDetails.add(builder.build());
                        }
                    }
                    results.put(module, deployDetails);
                } catch (NoSuchAlgorithmException e) {
                    listener.error("Could not find checksum algorithm for " + SHA1 + " or " + MD5);
                    isSuccess = false;
                }
            }
            return isSuccess ? results : null;
        }
    }
}
