package org.jfrog.hudson.pipeline.common;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.AbstractBuildInfoDeployer;
import org.jfrog.hudson.BuildInfoResultAction;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.plugins.PluginsUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                             Run build, TaskListener listener, BuildInfoAccessor buildinfoAccessor) throws IOException, InterruptedException, NoSuchAlgorithmException {
        super(configurator, build, listener, client);
        this.configurator = configurator;
        this.build = build;
        envVars = buildinfoAccessor.getEnvVars();
        sysVars = buildinfoAccessor.getSysVars();
        buildInfo = createBuildInfo("Pipeline", "", BuildType.GENERIC);
        buildInfo.setBuildRetention(buildinfoAccessor.getRetention().createBuildRetention());
        asyncBuildRetention = buildinfoAccessor.getRetention().isAsync();

        if (buildinfoAccessor.getStartDate() != null) {
            buildInfo.setStartedDate(buildinfoAccessor.getStartDate());
        }

        buildInfo.setModules(new ArrayList<Module>(buildinfoAccessor.getModules()));
        this.buildInfo.setBuildDependencies(new ArrayList<BuildDependency>(buildinfoAccessor.getBuildDependencies()));

        if (StringUtils.isNotEmpty(buildinfoAccessor.getBuildName())) {
            buildInfo.setName(buildinfoAccessor.getBuildName());
        }

        if (StringUtils.isNotEmpty(buildinfoAccessor.getBuildNumber())) {
            buildInfo.setNumber(buildinfoAccessor.getBuildNumber());
        }
        addVcsDataToBuild(build);
    }

    private void addVcsDataToBuild(Run build) {
        if (Jenkins.getInstance().getPlugin(PluginsUtils.GIT_PLUGIN_ID) == null) {
            return;
        }
        List<Vcs> vcsList = Utils.extractVcsBuildData(build);
        buildInfo.setVcs(vcsList);
        if (!vcsList.isEmpty()) {
            Vcs lastVcs = vcsList.get(vcsList.size() - 1);
            buildInfo.setVcsUrl(lastVcs.getUrl());
            buildInfo.setVcsRevision(lastVcs.getRevision());
        }
    }

    public void deploy() throws IOException {
        String artifactoryUrl = configurator.getArtifactoryServer().getUrl();
        listener.getLogger().println("Deploying build info to: " + artifactoryUrl + "/api/build");
        BuildRetention retention = buildInfo.getBuildRetention();
        buildInfo.setBuildRetention(null);
        org.jfrog.build.extractor.retention.Utils.sendBuildAndBuildRetention(client, this.buildInfo, retention, asyncBuildRetention);
        addBuildInfoResultAction(artifactoryUrl);
    }

    private void addBuildInfoResultAction(String artifactoryUrl) {
        synchronized (build.getActions()) {
            BuildInfoResultAction action = build.getAction(BuildInfoResultAction.class);
            if (action == null) {
                action = new BuildInfoResultAction(build);
                build.getActions().add(action);
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
