package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;

/**
 * @author yahavi
 */
public class MavenGradleEnvExtractor extends EnvExtractor {

    public MavenGradleEnvExtractor(Run build, BuildInfo buildInfo, Deployer publisher, Resolver resolver, TaskListener buildListener, Launcher launcher, FilePath tempDir, EnvVars env) {
        super(build, buildInfo, publisher, resolver, buildListener, launcher, tempDir, env);
    }

    @Override
    protected void addExtraConfiguration(ArtifactoryClientConfiguration configuration) {
    }
}
