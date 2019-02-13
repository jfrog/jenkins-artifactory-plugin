package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.builder.DistributionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.types.DistributionConfig;
import org.jfrog.hudson.release.DistributionUtils;
import org.jfrog.hudson.util.CredentialManager;

import java.io.IOException;

/**
 * Created by yahavi on 12/04/2017.
 */

public class DistributionExecutor implements Executor {

    private final ArtifactoryServer server;
    private final TaskListener listener;
    private final DistributionConfig distributionConfig;
    private final StepContext context;
    private Run build;

    public DistributionExecutor(ArtifactoryServer server, Run build, TaskListener listener, StepContext context,
                                DistributionConfig distributionConfig) {
        this.server = server;
        this.build = build;
        this.listener = listener;
        this.context = context;
        this.distributionConfig = distributionConfig;
    }

    public void execute() throws IOException {
        ArtifactoryConfigurator configurator = new ArtifactoryConfigurator(server);
        CredentialsConfig deployerConfig = CredentialManager.getPreferredDeployer(configurator, server);
        ArtifactoryBuildInfoClient client = server.createArtifactoryClient(deployerConfig.provideUsername(build.getParent()), deployerConfig.providePassword(build.getParent()),
                ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy));

        DistributionBuilder distributionBuilder = new DistributionBuilder()
                .publish(distributionConfig.isPublish())
                .overrideExistingFiles(distributionConfig.isOverrideExistingFiles())
                .gpgPassphrase(distributionConfig.getGpgPassphrase())
                .async(distributionConfig.isAsync())
                .targetRepo(distributionConfig.getTargetRepo())
                .sourceRepos(distributionConfig.getSourceRepos())
                .dryRun(distributionConfig.isDryRun());

        logInfo();

        boolean status = DistributionUtils.distributeAndCheckResponse(distributionBuilder, client, listener,
                distributionConfig.getBuildName(), distributionConfig.getBuildNumber(), distributionConfig.isDryRun());
        if (!status) {
            context.onFailure(new Exception("Build distribution failed"));
        }
    }

    private void logInfo() {
        StringBuilder strBuilder = new StringBuilder()
                .append("Distributing '").append(distributionConfig.getBuildName()).append("' ")
                .append("#").append(distributionConfig.getBuildNumber())
                .append(" to '").append(distributionConfig.getTargetRepo()).append("'");

        if (distributionConfig.getSourceRepos() != null) {
            strBuilder.append(" from '").append(JSONArray.fromObject(distributionConfig.getSourceRepos()).toString()).append("'");
        }

        if (distributionConfig.isPublish()) {
            strBuilder.append(", publishing");
        }

        if (StringUtils.isNotEmpty(distributionConfig.getGpgPassphrase())) {
            strBuilder.append(", using GPG passphrase");
        }

        if (distributionConfig.isOverrideExistingFiles()) {
            strBuilder.append(", overriding existing files");
        }

        if (distributionConfig.isAsync()) {
            strBuilder.append(", async");
        }

        if (distributionConfig.isDryRun()) {
            strBuilder.append(", dry run");
        }

        listener.getLogger().println(strBuilder.append(".").toString());
    }
}