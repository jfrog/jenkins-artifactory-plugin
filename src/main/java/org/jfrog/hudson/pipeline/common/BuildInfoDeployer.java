package org.jfrog.hudson.pipeline.common;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.AbstractBuildInfoDeployer;
import org.jfrog.hudson.BuildInfoResultAction;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.plugins.PluginsUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by romang on 4/25/16.
 */
public class BuildInfoDeployer extends AbstractBuildInfoDeployer {

    private final Run build;
    private final Map<String, String> sysVars;
    private final Map<String, String> envVars;
    private ArtifactoryConfigurator configurator;
    private Build buildInfo;
    private boolean asyncBuildRetention;

    public BuildInfoDeployer(ArtifactoryConfigurator configurator, ArtifactoryBuildInfoClient client,
                             Run build, TaskListener listener, BuildInfo deployedBuildInfo) throws IOException, InterruptedException, NoSuchAlgorithmException {
        super(configurator, build, listener, client);
        this.configurator = configurator;
        this.build = build;
        envVars = deployedBuildInfo.getEnvVars();
        sysVars = deployedBuildInfo.getSysVars();
        buildInfo = createBuildInfo("Pipeline", "");
        buildInfo.setBuildRetention(deployedBuildInfo.getRetention().createBuildRetention());
        asyncBuildRetention = deployedBuildInfo.getRetention().isAsync();

        if (deployedBuildInfo.getStartDate() != null) {
            buildInfo.setStartedDate(deployedBuildInfo.getStartDate());
        }

        buildInfo.setModules(new ArrayList<Module>(deployedBuildInfo.getModules()));

        if (StringUtils.isNotEmpty(deployedBuildInfo.getName())) {
            buildInfo.setName(deployedBuildInfo.getName());
        }

        if (StringUtils.isNotEmpty(deployedBuildInfo.getNumber())) {
            buildInfo.setNumber(deployedBuildInfo.getNumber());
        }

        if (deployedBuildInfo.getIssues() != null && !deployedBuildInfo.getConvertedIssues().isEmpty()) {
            buildInfo.setIssues(deployedBuildInfo.getConvertedIssues());
        }

        addVcsDataToBuild(build, deployedBuildInfo);
    }

    private void addVcsDataToBuild(Run build, BuildInfo deployedBuildInfo) {
        List<Vcs> vcsList = getVcsFromGitPlugin(build);

        // If collected VCS in a different flow
        if (CollectionUtils.isNotEmpty(deployedBuildInfo.getVcs())) {
            vcsList.addAll(deployedBuildInfo.getVcs());
        }

        // Keep only distinct values
        vcsList = vcsList.stream().distinct().collect(Collectors.toList());
        buildInfo.setVcs(vcsList);
    }

    private List<Vcs> getVcsFromGitPlugin(Run build) {
        if (Jenkins.get().getPlugin(PluginsUtils.GIT_PLUGIN_ID) == null) {
            return new ArrayList<>();
        }
        List<Vcs> vcsList = Utils.extractVcsBuildData(build);
        return vcsList;
    }

    public void deploy() throws IOException {
        String artifactoryUrl = configurator.getArtifactoryServer().getArtifactoryUrl();
        listener.getLogger().println("Deploying build info to: " + artifactoryUrl + "/api/build");
        BuildRetention retention = buildInfo.getBuildRetention();
        buildInfo.setBuildRetention(null);
        org.jfrog.build.extractor.retention.Utils.sendBuildAndBuildRetention(client, this.buildInfo, retention, asyncBuildRetention);
        addBuildInfoResultAction(artifactoryUrl);
    }

    private void addBuildInfoResultAction(String artifactoryUrl) {
        synchronized (build.getAllActions()) {
            BuildInfoResultAction action = build.getAction(BuildInfoResultAction.class);
            if (action == null) {
                action = new BuildInfoResultAction(build);
                build.addAction(action);
            }
            action.addBuildInfoResults(artifactoryUrl, buildInfo);
        }
    }

    /**
     * Adding environment and system variables to build info.
     *
     * @param builder
     */
    @Override
    protected void addBuildInfoProperties(BuildInfoBuilder builder) {
        if (envVars != null) {
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                builder.addProperty(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        if (sysVars != null) {
            for (Map.Entry<String, String> entry : sysVars.entrySet()) {
                builder.addProperty(entry.getKey(), entry.getValue());
            }
        }
    }
}
