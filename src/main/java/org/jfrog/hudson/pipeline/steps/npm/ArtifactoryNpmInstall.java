package org.jfrog.hudson.pipeline.steps.npm;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.EnvExtractor;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public class ArtifactoryNpmInstall extends AbstractStepImpl {
    private static final ArtifactoryVersion MIN_SUPPORTED_NPM_VERSION = new ArtifactoryVersion("5.4.0");
    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String rootDir;

    @DataBoundConstructor
    public ArtifactoryNpmInstall(NpmBuild npmBuild, String rootDir, BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
        this.npmBuild = npmBuild;
        this.rootDir = Objects.toString(rootDir, "");
    }

    private NpmBuild getNpmBuild() {
        return npmBuild;
    }

    private BuildInfo getBuildInfo() {
        return buildInfo;
    }

    private String getRootDir() {
        return rootDir;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient ArtifactoryNpmInstall step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        private transient EnvVars extendedEnv;

        private transient FilePath tempDir;

        @Override
        protected BuildInfo run() throws Exception {
            checkVersion();
            ArgumentListBuilder args = getNpmExecutor();
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            Deployer deployer = step.getNpmBuild().getDeployer();
            deployer.createPublisherBuildInfoDetails(buildInfo);
            String revision = Utils.extractVcsRevision(new FilePath(ws, step.getRootDir()));
            extendedEnv = new EnvVars(env);
            extendedEnv.put(ExtractorUtils.GIT_COMMIT, revision);
            EnvExtractor envExtractor = new EnvExtractor(build,
                    buildInfo, deployer, step.getNpmBuild().getResolver(), listener, launcher);
            tempDir = ExtractorUtils.createAndGetTempDir(launcher, ws);

            envExtractor.buildEnvVars(tempDir, extendedEnv);
            exe(args);
            String generatedBuildPath = extendedEnv.get(BuildInfoFields.GENERATED_BUILD_INFO);
            buildInfo.append(Utils.getGeneratedBuildInfo(build, listener, launcher, generatedBuildPath));
            // Read the deployable artifacts list from the 'json' file in the agent and append them to the buildInfo object.
            buildInfo.appendDeployableArtifacts(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS), tempDir, listener);
            buildInfo.setAgentName(Utils.getAgentName(ws));
            return buildInfo;
        }

        private ArgumentListBuilder getNpmExecutor() {
            ArgumentListBuilder args = new ArgumentListBuilder("npm", "install");
            if (!launcher.isUnix()) {
                args = args.toWindowsCommand();
            }
            return args;
        }


        private void exe(ArgumentListBuilder args) {
            boolean failed = false;
            try {
                failed = launcher.launch().cmds(args).envs(extendedEnv).stdout(listener).pwd(ws).join() != 0;
            } catch (Exception e) {
                throwError("Couldn't execute npm task. " + e.getMessage());
            }
            if (failed) {
                throwError("npm task failed");
            }
        }

        private void checkVersion() throws IOException, InterruptedException {
            ArgumentListBuilder versionArguments = new ArgumentListBuilder("npm", "-version");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int exitValue = launcher.launch().cmds(versionArguments).stdout(byteArrayOutputStream).join();
            if (exitValue != 0) {
                throwError("Could not find 'npm' executable in path");
            }
            ArtifactoryVersion npmVersion = new ArtifactoryVersion(StringUtils.trim(byteArrayOutputStream.toString()));
            if (!npmVersion.isAtLeast(MIN_SUPPORTED_NPM_VERSION)) {
                throwError("Couldn't execute npm task. Version must be at least " + MIN_SUPPORTED_NPM_VERSION.toString() + ".");
            }
        }


        private void throwError(String error) {
            listener.error(error);
            build.setResult(Result.FAILURE);
            throw new Run.RunnerAbortedException();
        }

    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ArtifactoryNpmInstall.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "ArtifactoryNpmInstall";
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
