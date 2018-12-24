package org.jfrog.hudson.pipeline.common.types.buildInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.client.DeployableArtifactDetail;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployableArtifactsUtils;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.BuildInfoDeployer;
import org.jfrog.hudson.pipeline.common.docker.proxy.BuildInfoProxy;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by romang on 4/26/16.
 */
public class BuildInfo implements Serializable {
    public static final long serialVersionUID = 1L;

    private String name; // Build name
    private String number; // Build number
    private Date startDate;
    private BuildRetention retention;
    private List<BuildDependency> buildDependencies = Collections.synchronizedList(new ArrayList<BuildDependency>());
    private List<Artifact> deployedArtifacts = Collections.synchronizedList(new ArrayList<Artifact>());
    // The candidates artifacts to be deployed in the 'deployArtifacts' step.
    private List<DeployDetails> deployableArtifacts = Collections.synchronizedList(new ArrayList<DeployDetails>());
    private List<Dependency> publishedDependencies = Collections.synchronizedList(new ArrayList<Dependency>());

    private List<Module> modules = Collections.synchronizedList(new ArrayList<Module>());
    private Env env = new Env();
    private String agentName;

    private transient DockerBuildInfoHelper dockerBuildInfoHelper = new DockerBuildInfoHelper(this);

    // Default constructor to allow serialization
    public BuildInfo() {
        this.startDate = Calendar.getInstance().getTime();
        this.retention = new BuildRetention();
    }

    public BuildInfo(Run build) {
        this();
        this.name = BuildUniqueIdentifierHelper.getBuildName(build);
        this.number = BuildUniqueIdentifierHelper.getBuildNumber(build);
    }

    @Whitelisted
    public void setName(String name) {
        this.name = name;
    }

    @Whitelisted
    public void setNumber(String number) {
        this.number = number;
    }

    @Whitelisted
    public String getName() {
        return name;
    }

    @Whitelisted
    public String getNumber() {
        return number;
    }

    @Whitelisted
    public Date getStartDate() {
        return startDate;
    }

    @Whitelisted
    public void setStartDate(Date date) {
        this.startDate = date;
    }

    @Whitelisted
    public void append(BuildInfo other) {
        this.modules.addAll(other.modules);
        this.deployedArtifacts.addAll(other.deployedArtifacts);
        this.deployableArtifacts.addAll(other.deployableArtifacts);
        this.publishedDependencies.addAll(other.publishedDependencies);
        this.buildDependencies.addAll(other.buildDependencies);
        this.dockerBuildInfoHelper.append(other.dockerBuildInfoHelper);

        Env tempEnv = new Env();
        tempEnv.append(this.env);
        tempEnv.append(other.env);
        this.env = tempEnv;
    }

    public void append(Build other) {
        Properties properties = other.getProperties();
        Env otherEnv = new Env();
        if (properties != null) {
            for (String key : properties.stringPropertyNames()) {
                boolean isEnvVar = StringUtils.startsWith(key, BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX);
                if (isEnvVar) {
                    otherEnv.getEnvVars().put(StringUtils.substringAfter(key, BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX), properties.getProperty(key));
                } else {
                    otherEnv.getSysVars().put(key, properties.getProperty(key));
                }
            }
            this.env.append(otherEnv);
        }
        if (other.getModules() != null) {
            this.modules.addAll(other.getModules());
        }
        if (other.getBuildDependencies() != null) {
            this.buildDependencies.addAll(other.getBuildDependencies());
        }
    }

    @Whitelisted
    public Env getEnv() {
        return env;
    }

    @Whitelisted
    public BuildRetention getRetention() {
        return retention;
    }

    @Whitelisted
    public void retention(Map<String, Object> retentionArguments) throws Exception {
        Set<String> retentionArgumentsSet = retentionArguments.keySet();
        List<String> keysAsList = Arrays.asList(new String[]{"maxDays", "maxBuilds", "deleteBuildArtifacts", "doNotDiscardBuilds", "async"});
        if (!keysAsList.containsAll(retentionArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        final ObjectMapper mapper = new ObjectMapper();
        this.retention = mapper.convertValue(retentionArguments, BuildRetention.class);
    }

    protected void appendDeployedArtifacts(List<Artifact> artifacts) {
        if (artifacts == null) {
            return;
        }
        deployedArtifacts.addAll(artifacts);
    }

    public List<DeployDetails> getDeployableArtifacts() {
        return deployableArtifacts;
    }

    public void appendDeployableArtifacts(String deployableArtifactsPath, FilePath ws, TaskListener listener) throws IOException, InterruptedException {
        List<DeployDetails> deployableArtifacts = ws.act(new DeployPathsAndPropsCallable(deployableArtifactsPath, listener, this));
        this.deployableArtifacts.addAll(deployableArtifacts);
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    protected void appendBuildDependencies(List<BuildDependency> dependencies) {
        if (dependencies == null) {
            return;
        }
        buildDependencies.addAll(dependencies);
    }

    protected void appendPublishedDependencies(List<Dependency> dependencies) {
        if (dependencies == null) {
            return;
        }
        publishedDependencies.addAll(dependencies);
    }

    protected List<BuildDependency> getBuildDependencies() {
        return buildDependencies;
    }

    protected Map<String, String> getEnvVars() {
        return env.getEnvVars();
    }

    protected Map<String, String> getSysVars() {
        return env.getSysVars();
    }

    protected BuildInfoDeployer createDeployer(Run build, TaskListener listener, ArtifactoryConfigurator config, ArtifactoryBuildInfoClient client)
            throws InterruptedException, NoSuchAlgorithmException, IOException {
        if (BuildInfoProxy.isUp()) {
            List<Module> dockerModules = dockerBuildInfoHelper.generateBuildInfoModules(build, listener, config);
            addDockerBuildInfoModules(dockerModules);
        }

        addDefaultModuleToModules(name);
        return new BuildInfoDeployer(config, client, build, listener, new BuildInfoAccessor(this));
    }

    private void addDockerBuildInfoModules(List<Module> dockerModules) {
        modules.addAll(dockerModules);
    }

    private void addDefaultModuleToModules(String moduleId) {
        if (deployedArtifacts.isEmpty() && publishedDependencies.isEmpty()) {
            return;
        }

        ModuleBuilder moduleBuilder = new ModuleBuilder()
                .id(moduleId)
                .artifacts(deployedArtifacts)
                .dependencies(publishedDependencies);
        modules.add(moduleBuilder.build());
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.env.setCpsScript(cpsScript);
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setRetention(BuildRetention retention) {
        this.retention = retention;
    }

    public void setBuildDependencies(List<BuildDependency> buildDependencies) {
        this.buildDependencies = buildDependencies;
    }

    public List<Artifact> getDeployedArtifacts() {
        return deployedArtifacts;
    }

    public void setDeployedArtifacts(List<Artifact> deployedArtifacts) {
        this.deployedArtifacts = deployedArtifacts;
    }

    public void setDeployableArtifacts(List<DeployDetails> deployableArtifacts) {
        this.deployableArtifacts = deployableArtifacts;
    }

    public List<Dependency> getPublishedDependencies() {
        return publishedDependencies;
    }

    public void setPublishedDependencies(List<Dependency> publishedDependencies) {
        this.publishedDependencies = publishedDependencies;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    public void setEnv(Env env) {
        this.env = env;
    }

    public static class DeployPathsAndPropsCallable extends MasterToSlaveFileCallable<List<DeployDetails>> {
        private String deployableArtifactsPath;
        private TaskListener listener;
        private ArrayListMultimap<String, String> propertiesMap;

        public DeployPathsAndPropsCallable(String deployableArtifactsPath, TaskListener listener, BuildInfo buildInfo) {
            this.deployableArtifactsPath = deployableArtifactsPath;
            this.listener = listener;
            this.propertiesMap = getbuildPropertiesMap(buildInfo);
        }

        public List<DeployDetails> invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
            try {
                List<DeployDetails> results = new ArrayList<DeployDetails>();
                List<DeployableArtifactDetail> deployableArtifacts = new ArrayList<DeployableArtifactDetail>();
                File deployableArtifactsFile = new File(deployableArtifactsPath);
                deployableArtifacts.addAll(DeployableArtifactsUtils.loadDeployableArtifactsFromFile(deployableArtifactsFile));
                deployableArtifactsFile.delete();
                for (DeployableArtifactDetail artifact : deployableArtifacts) {
                    DeployDetails.Builder builder = new DeployDetails.Builder()
                            .file(new File(artifact.getSourcePath()))
                            .artifactPath(artifact.getArtifactDest())
                            .addProperties(propertiesMap)
                            .targetRepository("empty_repo")
                            .sha1(artifact.getSha1());
                    results.add(builder.build());
                }
                return results;
            } catch (ClassNotFoundException e) {
                listener.getLogger().println(e.getMessage());
                return new ArrayList<DeployDetails>();
            }
        }

        private ArrayListMultimap<String, String> getbuildPropertiesMap(BuildInfo buildInfo) {
            ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
            properties.put("build.name", buildInfo.getName());
            properties.put("build.number", buildInfo.getNumber());
            properties.put("build.timestamp", buildInfo.getStartDate().getTime() + "");
            return properties;
        }
    }
}
