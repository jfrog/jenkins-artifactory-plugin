package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.types.PromotionConfig;
import org.jfrog.hudson.release.PromotionUtils;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.ProxyUtils;

import java.io.IOException;

/**
 * Created by romang on 6/21/16.
 */
public class PromotionExecutor implements Executor {

    private final ArtifactoryServer server;
    private final TaskListener listener;
    private final PromotionConfig promotionConfig;
    private final StepContext context;
    private Run build;

    public PromotionExecutor(ArtifactoryServer server, Run build, TaskListener listener, StepContext context,
                             PromotionConfig promotionConfig) {
        this.server = server;
        this.build = build;
        this.listener = listener;
        this.context = context;
        this.promotionConfig = promotionConfig;
    }

    public void execute() throws IOException {
        ArtifactoryConfigurator configurator = new ArtifactoryConfigurator(server);
        CredentialsConfig deployerConfig = CredentialManager.getPreferredDeployer(configurator, server);
        ArtifactoryBuildInfoClient client = server.createArtifactoryClient(deployerConfig.provideCredentials(build.getParent()),
                ProxyUtils.createProxyConfiguration());

        PromotionBuilder promotionBuilder = new PromotionBuilder()
                .status(promotionConfig.getStatus())
                .comment(promotionConfig.getComment())
                .targetRepo(promotionConfig.getTargetRepo())
                .sourceRepo(promotionConfig.getSourceRepo())
                .dependencies(promotionConfig.isIncludeDependencies())
                .copy(promotionConfig.isCopy())
                .failFast(promotionConfig.isFailFast());

        logInfo();

        boolean status = PromotionUtils.promoteAndCheckResponse(promotionBuilder.build(), client, listener,
                promotionConfig.getBuildName(), promotionConfig.getBuildNumber());
        if (!status) {
            context.onFailure(new Exception("Build promotion failed"));
        }
    }

    private void logInfo() {

        StringBuilder strBuilder = new StringBuilder()
                .append("Promoting '").append(promotionConfig.getBuildName()).append("' ")
                .append("#").append(promotionConfig.getBuildNumber())
                .append(" to '").append(promotionConfig.getTargetRepo()).append("'");

        if (StringUtils.isNotEmpty(promotionConfig.getSourceRepo())) {
            strBuilder.append(" from '").append(promotionConfig.getSourceRepo()).append("'");
        }

        if (StringUtils.isNotEmpty(promotionConfig.getStatus())) {
            strBuilder.append(", with status: '").append(promotionConfig.getStatus()).append("'");
        }

        if (StringUtils.isNotEmpty(promotionConfig.getComment())) {
            strBuilder.append(", with comment: '").append(promotionConfig.getComment()).append("'");
        }

        if (promotionConfig.isIncludeDependencies()) {
            strBuilder.append(", including dependencies");
        }

        if (promotionConfig.isCopy()) {
            strBuilder.append(", using copy");
        }

        if (promotionConfig.isFailFast()) {
            strBuilder.append(", failing on first error");
        }

        listener.getLogger().println(strBuilder.append(".").toString());
    }
}