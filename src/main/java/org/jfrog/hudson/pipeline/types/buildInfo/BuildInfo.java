package org.jfrog.hudson.pipeline.types.buildInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.client.DeployableArtifactDetail;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployableArtifactsUtils;
import org.jfrog.hudson.pipeline.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.BuildInfoDeployer;
import org.jfrog.hudson.pipeline.docker.proxy.BuildInfoProxy;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by romang on 4/26/16.
 */
public class BuildInfo implements Serializable {

    private String buildName;
    private String buildNumber;
    private Date startDate;
    private BuildRetention retention;
    private List<BuildDependency> buildDependencies = Collections.synchronizedList(new ArrayList<>());
    private List<Artifact> deployedArtifacts = Collections.synchronizedList(new ArrayList<>());
    // The candidates artifacts to be deployed in the 'deployArtifacts' step.
    private List<DeployDetails> deployableArtifacts = Collections.synchronizedList(new ArrayList<>());
    private List<Dependency> publishedDependencies = Collections.synchronizedList(new ArrayList<>());

    private List<Module> modules = Collections.synchronizedList(new ArrayList<>());
    private Env env = new Env();
    private String agentName;

    private DockerBuildInfoHelper dockerBuildInfoHelper = new DockerBuildInfoHelper(this);

    public BuildInfo(Run build) {
        this.buildName = BuildUniqueIdentifierHelper.getBuildName(build);
        this.buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
        this.startDate = Calendar.getInstance().getTime();
        this.retention = new BuildRetention();
    }

    @Whitelisted
    public void setName(String name) {
        this.buildName = name;
    }

    @Whitelisted
    public void setNumber(String number) {
        this.buildNumber = number;
    }

    @Whitelisted
    public String getName() {
        return buildName;
    }

    @Whitelisted
    public String getNumber() {
        return buildNumber;
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
    public List<org.jfrog.hudson.pipeline.types.File> getArtifacts() {
        Stream<Artifact> dependencyStream = Stream.concat(
                // Add modules artifacts
                modules.parallelStream().map(Module::getArtifacts).filter(Objects::nonNull).flatMap(List::stream),
                // Add deployed artifact
                deployedArtifacts.parallelStream()
        );
        return getBuildFilesList(dependencyStream);
    }

    @Whitelisted
    public List<org.jfrog.hudson.pipeline.types.File> getDependencies() {
        Stream<Dependency> dependencyStream = Stream.concat(
                // Add modules artifacts
                modules.parallelStream().map(Module::getDependencies).filter(Objects::nonNull).flatMap(List::stream),
                // Add deployed artifact
                publishedDependencies.parallelStream()
        );
        return getBuildFilesList(dependencyStream);
    }

    /**
     * Return a list of 'Files' of downloaded or uploaded files. Filters build files without local and remote paths.
     *
     * @param buildFilesStream - Stream of build Artifacts or Dependencies.
     * @return - List of build files.
     */
    private List<org.jfrog.hudson.pipeline.types.File> getBuildFilesList(Stream<? extends BaseBuildFileBean> buildFilesStream) {
        return buildFilesStream
                .filter(buildFile -> StringUtils.isNotBlank(buildFile.getLocalPath()))
                .filter(buildFile -> StringUtils.isNotBlank(buildFile.getRemotePath()))
                .map(org.jfrog.hudson.pipeline.types.File::new)
                .distinct()
                .collect(Collectors.toList());
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
    public void retention(Map<String, Object> retentionArguments) {
        Set<String> retentionArgumentsSet = retentionArguments.keySet();
        List<String> keysAsList = Arrays.asList("maxDays", "maxBuilds", "deleteBuildArtifacts", "doNotDiscardBuilds", "async");
        if (!keysAsList.containsAll(retentionArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        final ObjectMapper mapper = new ObjectMapper();
        this.retention = mapper.convertValue(retentionArguments, BuildRetention.class);
    }

    void appendDeployedArtifacts(List<Artifact> artifacts) {
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

    void appendPublishedDependencies(List<Dependency> dependencies) {
        if (dependencies == null) {
            return;
        }
        publishedDependencies.addAll(dependencies);
    }

    List<BuildDependency> getBuildDependencies() {
        return buildDependencies;
    }

    Map<String, String> getEnvVars() {
        return env.getEnvVars();
    }

    Map<String, String> getSysVars() {
        return env.getSysVars();
    }

    BuildInfoDeployer createDeployer(Run build, TaskListener listener, ArtifactoryConfigurator config, ArtifactoryBuildInfoClient client)
            throws InterruptedException, NoSuchAlgorithmException, IOException {
        if (BuildInfoProxy.isUp()) {
            List<Module> dockerModules = dockerBuildInfoHelper.generateBuildInfoModules(build, listener, config);
            addDockerBuildInfoModules(dockerModules);
        }

        addDefaultModuleToModules(buildName);
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

    public static class DeployPathsAndPropsCallable extends MasterToSlaveFileCallable<List<DeployDetails>> {
        private String deployableArtifactsPath;
        private TaskListener listener;
        private ArrayListMultimap<String, String> propertiesMap;

        DeployPathsAndPropsCallable(String deployableArtifactsPath, TaskListener listener, BuildInfo buildInfo) {
            this.deployableArtifactsPath = deployableArtifactsPath;
            this.listener = listener;
            this.propertiesMap = getbuildPropertiesMap(buildInfo);
        }

        public List<DeployDetails> invoke(File file, VirtualChannel virtualChannel) throws IOException {
            List<DeployDetails> results = new ArrayList<>();
            try {
                File deployableArtifactsFile = new File(deployableArtifactsPath);
                List<DeployableArtifactDetail> deployableArtifacts = DeployableArtifactsUtils.loadDeployableArtifactsFromFile(deployableArtifactsFile);
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
                ExceptionUtils.printRootCauseStackTrace(e, listener.getLogger());
            }
            return results;
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
