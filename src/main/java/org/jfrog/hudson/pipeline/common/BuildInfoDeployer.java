package org.jfrog.hudson.pipeline.common;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.ci.BuildInfoProperties;
import org.jfrog.build.extractor.ci.BuildRetention;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.ci.Vcs;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.hudson.AbstractBuildInfoDeployer;
import org.jfrog.hudson.BuildInfoResultAction;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Created by romang on 4/25/16.
 */
public class BuildInfoDeployer extends AbstractBuildInfoDeployer {

    private final Run build;
    private final Map<String, String> sysVars;
    private final Map<String, String> envVars;
    private ArtifactoryConfigurator configurator;
    private org.jfrog.build.extractor.ci.BuildInfo buildInfo;
    private boolean asyncBuildRetention;
    private final String platformUrl;

    public BuildInfoDeployer(ArtifactoryConfigurator configurator, ArtifactoryManager artifactoryManager,
                             Run build, TaskListener listener, BuildInfo deployedBuildInfo, String platformUrl) throws IOException, InterruptedException {
        super(configurator, build, listener, artifactoryManager);
        this.configurator = configurator;
        this.build = build;
        this.platformUrl = platformUrl;
        envVars = deployedBuildInfo.getEnvVars();
        sysVars = deployedBuildInfo.getSysVars();
        buildInfo = createBuildInfo("Pipeline", "");
        buildInfo.setBuildRetention(deployedBuildInfo.getRetention().createBuildRetention());
        asyncBuildRetention = deployedBuildInfo.getRetention().isAsync();

        if (deployedBuildInfo.getStartDate() != null) {
            buildInfo.setStartedDate(deployedBuildInfo.getStartDate());
        }

        buildInfo.setModules(new CopyOnWriteArrayList<Module>(deployedBuildInfo.getModules()));

        if (StringUtils.isNotEmpty(deployedBuildInfo.getName())) {
            buildInfo.setName(deployedBuildInfo.getName());
        }

        if (StringUtils.isNotEmpty(deployedBuildInfo.getNumber())) {
            buildInfo.setNumber(deployedBuildInfo.getNumber());
        }

        buildInfo.setProject(deployedBuildInfo.getProject());

        if (deployedBuildInfo.getIssues() != null && !deployedBuildInfo.getConvertedIssues().isEmpty()) {
            buildInfo.setIssues(deployedBuildInfo.getConvertedIssues());
        }

        addVcsDataToBuild(deployedBuildInfo);
    }

    private void addVcsDataToBuild(BuildInfo deployedBuildInfo) {
        if (CollectionUtils.isEmpty(deployedBuildInfo.getVcs())) {
            return;
        }
        List<Vcs> vcsList = deployedBuildInfo.getVcs();
        // Keep only distinct values
        vcsList = vcsList.stream().distinct().collect(Collectors.toList());
        buildInfo.setVcs(vcsList);
    }

    public void deploy() throws IOException {
        String artifactoryUrl = configurator.getArtifactoryServer().getArtifactoryUrl();
        BuildRetention retention = buildInfo.getBuildRetention();
        buildInfo.setBuildRetention(null);
        org.jfrog.build.extractor.retention.Utils.sendBuildAndBuildRetention(artifactoryManager, this.buildInfo, retention, asyncBuildRetention, platformUrl);
        addBuildInfoResultAction(artifactoryUrl);
    }

    private void addBuildInfoResultAction(String artifactoryUrl) {
        synchronized (build.getAllActions()) {
            BuildInfoResultAction action = build.getAction(BuildInfoResultAction.class);
            if (action == null) {
                action = new BuildInfoResultAction(build);
                build.addAction(action);
            }
            action.addBuildInfoResults(artifactoryUrl, platformUrl, buildInfo);
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
