package org.jfrog.hudson.pipeline.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.types.resolvers.Resolver;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.ResolverContext;
import org.jfrog.hudson.util.publisher.PublisherContext;

import java.io.IOException;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class MavenGradleEnvExtractor {

    private Deployer publisher;
    private Resolver resolver;
    private Run build;
    private BuildInfo buildInfo;
    private TaskListener buildListener;
    private String propertiesFilePath;
    private Launcher launcher;

    public MavenGradleEnvExtractor(Run build, BuildInfo buildInfo, Deployer publisher, Resolver resolver, TaskListener buildListener, Launcher launcher)
            throws IOException, InterruptedException {
        this.build = build;
        this.buildInfo = buildInfo;
        this.buildListener = buildListener;
        this.publisher = publisher;
        this.resolver = resolver;
        this.launcher = launcher;
    }

    protected PublisherContext createPublisherContext() {
        return publisher.getContextBuilder().build();
    }

    public void buildEnvVars(FilePath ws, EnvVars env) throws Exception {
        env.put(ExtractorUtils.EXTRACTOR_USED, "true");
        ReleaseAction release = ActionableHelper.getLatestAction(build, ReleaseAction.class);
        if (release != null) {
            release.addVars(env);
        }
        try {
            PublisherContext publisherContext = null;
            // publisher should never be null or empty
            publisherContext = createPublisherContext();
            ResolverContext resolverContext = null;
            if (resolver != null && !resolver.isEmpty()) {
                CredentialsConfig resolverCredentials = CredentialManager.getPreferredResolver(resolver,
                        resolver.getArtifactoryServer());
                resolverContext = new ResolverContext(resolver.getArtifactoryServer(), resolver.getResolverDetails(),
                        resolverCredentials.getCredentials(build.getParent()), resolver);
            }

            ArtifactoryClientConfiguration configuration = ExtractorUtils.getArtifactoryClientConfiguration(
                    env, build, buildInfo, buildListener, publisherContext, resolverContext, ws);
            addPipelineInfoToConfiguration(env, configuration);
            ExtractorUtils.persistConfiguration(configuration, env, ws, launcher);
            propertiesFilePath = configuration.getPropertiesFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        env.put(BuildInfoConfigProperties.PROP_PROPS_FILE, propertiesFilePath);
    }

    private void addPipelineInfoToConfiguration(EnvVars env, ArtifactoryClientConfiguration configuration) {
        String tempFile = "";
        try {
            tempFile = Utils.createTempBuildInfoFile(launcher);
        } catch (Exception e) {
            buildListener.error("Failed while generating temp build info file. " + e.getMessage());
            build.setResult(Result.FAILURE);
            throw new Run.RunnerAbortedException();
        }
        env.put(BuildInfoFields.GENERATED_BUILD_INFO, tempFile);
        configuration.info.setGeneratedBuildInfoFilePath(tempFile);
    }
}
