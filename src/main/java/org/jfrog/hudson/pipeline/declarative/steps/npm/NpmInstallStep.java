package org.jfrog.hudson.pipeline.declarative.steps.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.NpmInstallExecutor;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.NpmBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.NpmGoResolver;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.SerializationUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

/**
 * Run npm-install task.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class NpmInstallStep extends AbstractStepImpl {

    private NpmBuild npmBuild;
    private String customBuildNumber;
    private String customBuildName;
    private String resolverId;
    private String javaArgs; // Added to allow java remote debugging
    private String path;
    private String args;
    private String module;

    @DataBoundConstructor
    public NpmInstallStep() {
        this.npmBuild = new NpmBuild();
    }

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
    public void setPath(String path) {
        this.path = path;
    }

    @DataBoundSetter
    public void setModule(String module) {
        this.module = module;
    }

    @DataBoundSetter
    public void setArgs(String args) {
        this.args = args;
    }

    @DataBoundSetter
    public void setTool(String tool) {
        npmBuild.setTool(tool);
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient NpmInstallStep step;

        @Override
        protected Void run() throws Exception {
            BuildInfo buildInfo = DeclarativePipelineUtils.getBuildInfo(ws, build, step.customBuildName, step.customBuildNumber);
            setResolver(BuildUniqueIdentifierHelper.getBuildNumber(build));
            Utils.addNpmToPath(ws, listener, env, launcher, step.npmBuild.getTool());
            NpmInstallExecutor npmInstallExecutor = new NpmInstallExecutor(buildInfo, launcher, step.npmBuild, step.javaArgs, step.args, ws, step.path, step.module, env, listener, build);
            npmInstallExecutor.execute();
            DeclarativePipelineUtils.saveBuildInfo(npmInstallExecutor.getBuildInfo(), ws, build, new JenkinsBuildInfoLog(listener));
            return null;
        }

        private void setResolver(String buildNumber) throws IOException, InterruptedException {
            if (StringUtils.isBlank(step.resolverId)) {
                return;
            }
            BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, NpmResolverStep.STEP_NAME, step.resolverId);
            if (buildDataFile == null) {
                throw new IOException("Resolver " + step.resolverId + " doesn't exist!");
            }
            NpmGoResolver resolver = SerializationUtils.createMapper().treeToValue(buildDataFile.get(NpmResolverStep.STEP_NAME), NpmGoResolver.class);
            resolver.setServer(getArtifactoryServer(buildNumber, buildDataFile));
            step.npmBuild.setResolver(resolver);
        }

        private ArtifactoryServer getArtifactoryServer(String buildNumber, BuildDataFile buildDataFile) throws IOException, InterruptedException {
            JsonNode serverId = buildDataFile.get("serverId");
            if (serverId.isNull()) {
                throw new IllegalArgumentException("server ID is missing");
            }
            return DeclarativePipelineUtils.getArtifactoryServer(build, ws, getContext(), serverId.asText());
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmInstallStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtNpmInstall";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory npm install";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
