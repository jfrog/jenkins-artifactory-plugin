package org.jfrog.hudson.pipeline.declarative.steps.nuget;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.NugetRunExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NugetBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.SerializationUtils;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

abstract public class NugetRunStepBase extends AbstractStepImpl {

    protected NugetBuild nugetBuild;
    private String customBuildNumber;
    private String customBuildName;
    private String resolverId;
    private String javaArgs; // Added to allow java remote debugging
    private String args;
    private String module;

    public NugetRunStepBase() {
        this.nugetBuild = new NugetBuild();
    }

    abstract public String getResolverStepName();

    @DataBoundSetter
    public void setBuildNumber(String customBuildNumber) {
        this.customBuildNumber = customBuildNumber;
    }

    @DataBoundSetter
    public void setBuildName(String customBuildName) {
        this.customBuildName = customBuildName;
    }

    @DataBoundSetter
    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    @DataBoundSetter
    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    @DataBoundSetter
    public void setModule(String module) {
        this.module = module;
    }

    @DataBoundSetter
    public void setArgs(String args) {
        this.args = args;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final NugetRunStepBase step;

        @Inject
        public Execution(NugetRunStepBase step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(rootWs, build, step.customBuildName, step.customBuildNumber);
            setResolver(BuildUniqueIdentifierHelper.getBuildNumber(build));
            NugetRunExecutor nugetRunExecutor = new NugetRunExecutor(buildInfo, launcher, step.nugetBuild, step.javaArgs, step.args, ws, step.module, env, listener, build);
            nugetRunExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(nugetRunExecutor.getBuildInfo(), rootWs, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.resolverId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(rootWs, buildNumber, step.getResolverStepName(), step.resolverId);
            if (buildDataFile == null) {
                throw new IOException("Resolver " + step.resolverId + " doesn't exist!");
            }
            CommonResolver resolver = SerializationUtils.createMapper().treeToValue(buildDataFile.get(step.getResolverStepName()), CommonResolver.class);
            resolver.setServer(getArtifactoryServer(buildDataFile));
            step.nugetBuild.setResolver(resolver);
        }

        private ArtifactoryServer getArtifactoryServer(BuildDataFile buildDataFile) throws IOException, InterruptedException {
            JsonNode serverId = buildDataFile.get("serverId");
            if (serverId.isNull()) {
                throw new IllegalArgumentException("server ID is missing");
            }
            return DeclarativePipelineUtils.getArtifactoryServer(build, rootWs, getContext(), serverId.asText());
        }
    }
}
