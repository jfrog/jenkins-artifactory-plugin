package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NugetBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

public class NugetRunExecutor extends BuildInfoProcessRunner {

    private NugetBuild nugetBuild;
    private String nugetArgs;
    private Log logger;

    public NugetRunExecutor(BuildInfo buildInfo, Launcher launcher, NugetBuild nugetBuild, String javaArgs, String nugetArgs, FilePath ws, String module, EnvVars env, TaskListener listener, Run build) {
        // For NuGet command the path parameter is irrelevant, so an empty string is passed.
        super(buildInfo, launcher, javaArgs, ws, StringUtils.EMPTY, module, env, listener, build);
        this.nugetBuild = nugetBuild;
        this.nugetArgs = nugetArgs;
        this.logger = new JenkinsBuildInfoLog(listener);
    }

    @Override
    public void execute() throws Exception {
        CommonResolver resolver = (CommonResolver) nugetBuild.getResolver();
        if (resolver.isEmpty()) {
            throw new IllegalStateException("Resolver must be configured with resolution repository and Artifactory server");
        }
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new NugetEnvExtractor(build, buildInfo, resolver, listener, launcher, tempDir, env, nugetArgs, module, nugetBuild.useDotnetCli());
        super.execute("nuget", "org.jfrog.build.extractor.nuget.extractor.NugetRun", envExtractor, tempDir);
    }
}
