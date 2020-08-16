package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;

public class NugetEnvExtractor extends EnvExtractor {
    private String args;
    private String module;
    private boolean useDotnetCli;

    public NugetEnvExtractor(Run build, BuildInfo buildInfo, Resolver resolver,
                             TaskListener buildListener, Launcher launcher, FilePath tempDir,
                             EnvVars env, String args, String module, boolean useDotnetCli) {
        super(build, buildInfo, null, resolver, buildListener, launcher, tempDir, env);
        this.args = args;
        this.module = module;
        this.useDotnetCli = useDotnetCli;
    }

    @Override
    protected void addExtraConfiguration(ArtifactoryClientConfiguration configuration) {
        configuration.packageManagerHandler.setArgs(args);
        configuration.packageManagerHandler.setModule(module);
        configuration.dotnetHandler.setUseDotnetCli(useDotnetCli);
    }
}
