package org.jfrog.hudson.pipeline.common.types.buildInfo;

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
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.DeployableArtifactDetail;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployableArtifactsUtils;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.BuildInfoDeployer;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by romang on 4/26/16.
 */
public class BuildInfo implements Serializable {
    public static final long serialVersionUID = 1L;

    private String name; // Build name
    private String number; // Build number
    private Date startDate;
    private BuildRetention retention;
    // The candidates artifacts to be deployed in the 'deployArtifacts' step.
    private List<DeployDetails> deployableArtifacts = new CopyOnWriteArrayList<>();
    private List<Vcs> vcs = new ArrayList<>();
    private List<Module> modules = new CopyOnWriteArrayList<>();
    private Env env = new Env();
    private Issues issues = new Issues();
    private String agentName;

    // Default constructor to allow serialization
    public BuildInfo() {
        this.startDate = Calendar.getInstance().getTime();
        this.retention = new BuildRetention();
    }

    public BuildInfo(Run build) {
        this();
        this.name = BuildUniqueIdentifierHelper.getBuildName(build);
        this.number = BuildUniqueIdentifierHelper.getBuildNumber(build);
        this.issues.setBuildName(name);
    }

    @Whitelisted
    public void setName(String name) {
        this.name = name;
        this.issues.setBuildName(name);
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
    public List<org.jfrog.hudson.pipeline.types.File> getArtifacts() {
        Stream<Artifact> dependencyStream = modules.parallelStream()
                .map(Module::getArtifacts)
                .filter(Objects::nonNull)
                .flatMap(List::stream);
        return getBuildFilesList(dependencyStream);
    }

    @Whitelisted
    public List<org.jfrog.hudson.pipeline.types.File> getDependencies() {
        Stream<Dependency> dependencyStream = modules.parallelStream()
                .map(Module::getDependencies)
                .filter(Objects::nonNull)
                .flatMap(List::stream);
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
        this.deployableArtifacts.addAll(other.deployableArtifacts);
        this.append(other.convertToBuild());
    }

    public void append(Build other) {
        Build appendedBuild = this.convertToBuild();
        appendedBuild.append(other);

        this.setModules(appendedBuild.getModules());

        /* Since we have different object to represent issues and environment vars in Build-info and Jenkins
         *  We need to  convert the appended Build-info's inner objects to Jenkins inner objects.
         */

        Issues appendedIssues = Issues.toPipelineIssues(appendedBuild.getIssues());
        appendedIssues.setBuildName(this.getIssues().getBuildName());
        appendedIssues.setCpsScript(this.getIssues().getCpsScript());
        this.setIssues(appendedIssues);

        Properties properties = appendedBuild.getProperties();
        Map<String, String> appendedEnvVar = new HashMap<>();
        Map<String, String> appendedSysVar = new HashMap<>();
        if (properties != null) {
            for (String key : properties.stringPropertyNames()) {
                boolean isEnvVar = StringUtils.startsWith(key, BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX);
                if (isEnvVar) {
                    appendedEnvVar.put(StringUtils.substringAfter(key, BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX), properties.getProperty(key));
                } else {
                    appendedSysVar.put(key, properties.getProperty(key));
                }
            }
            this.getEnv().setEnvVars(appendedEnvVar);
            this.getEnv().setSysVars(appendedSysVar);
        }
    }

    @Whitelisted
    public Env getEnv() {
        return env;
    }

    @Whitelisted
    public Issues getIssues() {
        return issues;
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

    Map<String, String> getEnvVars() {
        return env.getEnvVars();
    }

    Map<String, String> getSysVars() {
        return env.getSysVars();
    }

    org.jfrog.build.api.Issues getConvertedIssues() {
        return this.issues.convertFromPipelineIssues();
    }

    BuildInfoDeployer createDeployer(Run build, TaskListener listener, ArtifactoryConfigurator config, ArtifactoryBuildInfoClient client)
            throws InterruptedException, NoSuchAlgorithmException, IOException {
        return new BuildInfoDeployer(config, client, build, listener, new BuildInfoAccessor(this));
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.env.setCpsScript(cpsScript);
        this.issues.setCpsScript(cpsScript);
    }

    public List<Module> getModules() {
        return modules;
    }

    @SuppressWarnings("unused") // For serialization/deserialization
    public void setRetention(BuildRetention retention) {
        this.retention = retention;
    }

    @SuppressWarnings("unused") // For serialization/deserialization
    public void setDeployableArtifacts(List<DeployDetails> deployableArtifacts) {
        this.deployableArtifacts = deployableArtifacts;
    }

    @SuppressWarnings("unused") // For serialization/deserialization
    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    @SuppressWarnings("unused") // For serialization/deserialization
    public void setEnv(Env env) {
        this.env = env;
    }

    @SuppressWarnings("unused") // For serialization/deserialization
    public void setIssues(Issues issues) {
        this.issues = issues;
    }

    private void addModule(Module other) {
        List<Module> modules = getModules();
        Module currentModule = modules.stream()
                // Check if there's already a module with the same name.
                .filter(module -> StringUtils.equals(module.getId(), other.getId()))
                .findAny()
                .orElse(null);
        if (currentModule == null) {
            // Append new module.
            modules.add(other);
        } else {
            // Append the other module into the existing module with the same name.
            currentModule.append(other);
        }
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

    public void appendVcs(Vcs vcs) {
        if (vcs != null && !this.vcs.contains(vcs)) {
            this.vcs.add(vcs);
        }
    }

    public List<Vcs> getVcs() {
        return vcs;
    }

    private Build convertToBuild() {
        BuildInfoBuilder builder = new BuildInfoBuilder(name)
                .number(number)
                .started(Long.toString(startDate.getTime()))
                .modules(modules)
                .issues(getConvertedIssues())
                .properties(env.toProperties());

        return builder.build();
    }
}
