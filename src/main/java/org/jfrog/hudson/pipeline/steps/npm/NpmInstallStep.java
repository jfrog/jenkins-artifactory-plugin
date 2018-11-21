package org.jfrog.hudson.pipeline.steps.npm;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.EnvExtractor;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.NpmInstall;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Objects;

@SuppressWarnings("unused")
public class NpmInstallStep extends AbstractStepImpl {
    private static final ArtifactoryVersion MIN_SUPPORTED_NPM_VERSION = new ArtifactoryVersion("5.4.0");
    private static final String NPMRC_FILE_NAME = ".npmrc";
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private BuildInfo buildInfo;
    private NpmBuild npmBuild;
    private String rootDir;

    @DataBoundConstructor
    public NpmInstallStep(NpmBuild npmBuild, String rootDir, BuildInfo buildInfo) {
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
        private transient NpmInstallStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        private transient EnvVars extendedEnv;

        private transient FilePath tempDir;

        private transient ArtifactoryDependenciesClient dependenciesClient;

        private NpmInstall npmInstall;

        @Override
        protected BuildInfo run() throws Exception {
            npmInstall = new NpmInstall();
            extendedEnv = new EnvVars(env);
            ArtifactoryServer server = step.getNpmBuild().getResolver().getArtifactoryServer();
            CredentialsConfig preferredResolver = server.getResolverCredentialsConfig();
            dependenciesClient = server.createArtifactoryDependenciesClient(
                    preferredResolver.provideUsername(build.getParent()), preferredResolver.providePassword(build.getParent()),
                    ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy), listener);

            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());

            Deployer deployer = step.getNpmBuild().getDeployer();
            deployer.createPublisherBuildInfoDetails(buildInfo);
            String revision = Utils.extractVcsRevision(new FilePath(ws, step.getRootDir()));
            extendedEnv.put(ExtractorUtils.GIT_COMMIT, revision);
            EnvExtractor envExtractor = new EnvExtractor(build,
                    buildInfo, deployer, step.getNpmBuild().getResolver(), listener, launcher);
            tempDir = ExtractorUtils.createAndGetTempDir(launcher, ws);

            envExtractor.buildEnvVars(tempDir, extendedEnv);

            new BuildInfoAccessor(buildInfo).appendPublishedDependencies(npmInstall.getDependencies());

            // Read the deployable artifacts list from the 'json' file in the agent and append them to the buildInfo object.
            buildInfo.appendDeployableArtifacts(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS), tempDir, listener);
            buildInfo.setAgentName(Utils.getAgentName(ws));
            return buildInfo;
        }


    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(NpmInstallStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryNpmInstall";
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
