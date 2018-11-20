package org.jfrog.hudson.pipeline.steps.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.compress.utils.CharsetNames;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.extractor.NpmBuildInfoExtractor;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.EnvExtractor;
import org.jfrog.hudson.pipeline.types.NpmBuild;
import org.jfrog.hudson.pipeline.types.NpmInstall;
import org.jfrog.hudson.pipeline.types.NpmPackageInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.types.resolvers.NpmResolver;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

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
            // === Build Info code ===
            preparePrerequisites();
            createTempNpmrc();
            runInstall();
            restoreNpmrc();
            setDependenciesList();
            // =======================

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

            new BuildInfoAccessor(buildInfo).appendPublishedDependencies(npmInstall.getDependencies());

            // Read the deployable artifacts list from the 'json' file in the agent and append them to the buildInfo object.
            buildInfo.appendDeployableArtifacts(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS), tempDir, listener);
            buildInfo.setAgentName(Utils.getAgentName(ws));
            return buildInfo;
        }

        private String exe(ArgumentListBuilder args) throws UnsupportedEncodingException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            exe(args, byteArrayOutputStream);
            return byteArrayOutputStream.toString(CharsetNames.UTF_8).trim();
        }

        private void exe(ArgumentListBuilder args, OutputStream outputStream) {
            try {
                if (launcher.launch().cmds(args).envs(extendedEnv).stdout(outputStream).pwd(ws).join() != 0) {
                    throwError("npm task failed: " + outputStream.toString());
                }
            } catch (Exception e) {
                throwError("Couldn't execute npm task. " + e.getMessage());
            }
        }

        private void validateNpmVersion() throws UnsupportedEncodingException {
            ArgumentListBuilder versionArguments = new ArgumentListBuilder("npm", "-version");
            String npmVersionStr = exe(versionArguments);
            ArtifactoryVersion npmVersion = new ArtifactoryVersion(npmVersionStr);
            if (!npmVersion.isAtLeast(MIN_SUPPORTED_NPM_VERSION)) {
                throwError("Couldn't execute npm task. Version must be at least " + MIN_SUPPORTED_NPM_VERSION.toString() + ".");
            }
        }

        private void throwError(String error) {
            listener.error(error);
            build.setResult(Result.FAILURE);
            throw new Run.RunnerAbortedException();
        }


        // =========================== Prepare prerequisites ===========================
        private void preparePrerequisites() throws IOException, InterruptedException {
            setNpmExecutable();
            validateNpmVersion();
            setWorkingDirectory();
            setNpmAuth();
            setRegistryUrl();
            readPackageInfoFromPackageJson();
            backupProjectNpmrc();
        }

        private void setNpmExecutable() {
            npmInstall.setExecutablePath("npm");
        }

        private void setWorkingDirectory() {
            npmInstall.setWorkingDirectory(ws.getRemote());
        }

        private void setNpmAuth() throws IOException {
            npmInstall.setNpmAuth(dependenciesClient.getNpmAuth());
        }

        private void setRegistryUrl() {
            NpmResolver resolver = step.getNpmBuild().getResolver();
            if (!resolver.isEmpty()) {
                String npmRegistry = resolver.getResolverDetails().getArtifactoryUrl();
                if (!StringUtils.endsWith(npmRegistry, "/")) {
                    npmRegistry += "/";
                }
                npmRegistry += "api/npm" + resolver.getRepo();
                npmInstall.setRegistry(npmRegistry);
            }
        }

        private void readPackageInfoFromPackageJson() throws IOException, InterruptedException {
            NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
            npmPackageInfo.readPackageInfo(listener, ws);
            npmInstall.setNpmPackageInfo(npmPackageInfo);
        }

        /**
         * To make npm do the resolution from Artifactory we are creating .npmrc file in the project dir.
         * If a .npmrc file already exists we will backup it and override while running the command
         */
        private void backupProjectNpmrc() throws IOException, InterruptedException {
            ws.act(new MasterToSlaveFileCallable<Void>() {
                @Override
                public Void invoke(File f, VirtualChannel channel) throws IOException {
                    Path npmrcPath = f.toPath().resolve(NPMRC_FILE_NAME);
                    if (!Files.exists(npmrcPath)) {
                        return null;
                    }

                    Path npmrcBackupPath = f.toPath().resolve(NPMRC_BACKUP_FILE_NAME);
                    Files.copy(npmrcPath, npmrcBackupPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                    listener.getLogger().println("Project .npmrc file backed up successfully to " + npmrcBackupPath);
                    return null;
                }
            });
        }

        // =========================== Create Npmrc ===========================
        private void createTempNpmrc() throws IOException, InterruptedException {
            ws.child(NPMRC_FILE_NAME).delete(); // Delete old npmrc file
            listener.getLogger().println("Creating project .npmrc file");
            ArgumentListBuilder configListArgs = new ArgumentListBuilder(npmInstall.getExecutablePath(), "c", "ls", "--json");
            final String configList = exe(configListArgs);

            ws.act(new MasterToSlaveFileCallable<Void>() {
                @Override
                public Void invoke(File f, VirtualChannel channel) throws IOException {
                    Properties properties = new Properties();

                    // Save npm config list results
                    JsonNode manifestTree = Utils.mapper().readTree(configList);
                    manifestTree.fields().forEachRemaining(entry -> properties.setProperty(entry.getKey(), entry.getValue().asText()));

                    // Save npm auth
                    properties.putAll(npmInstall.getNpmAuth());

                    // Save registry
                    properties.setProperty("registry", npmInstall.getRegistry());
                    properties.remove("metrics-registry");

                    // Write npmrc file
                    File npmrcFile = f.toPath().resolve(NPMRC_FILE_NAME).toFile();
                    StringBuffer stringBuffer = new StringBuffer();
                    properties.forEach((key, value) -> stringBuffer.append(key).append("=").append(value).append("\n"));
                    try (FileWriter fileWriter = new FileWriter(npmrcFile);
                         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                        bufferedWriter.write(stringBuffer.toString());
                        bufferedWriter.flush();
                    }
                    return null;
                }
            });
        }

        // ======================== Run install ========================
        private void runInstall() {
            ArgumentListBuilder installArgs = new ArgumentListBuilder(npmInstall.getExecutablePath(), "i");
            exe(installArgs, listener.getLogger());
        }

        // ======================== Restore npmrc ========================
        private void restoreNpmrc() throws IOException, InterruptedException {
            ws.act(new MasterToSlaveFileCallable<Void>() {
                @Override
                public Void invoke(File f, VirtualChannel channel) throws IOException {
                    Path npmrcPath = f.toPath().resolve(NPMRC_FILE_NAME);
                    Path npmrcBackupPath = f.toPath().resolve(NPMRC_BACKUP_FILE_NAME);
                    if (!Files.exists(npmrcBackupPath)) {
                        Files.deleteIfExists(npmrcPath);
                        return null;
                    }

                    listener.getLogger().println("Restoring project .npmrc file");
                    Files.move(npmrcBackupPath, npmrcPath, StandardCopyOption.REPLACE_EXISTING);
                    listener.getLogger().println("Restored project .npmrc file successfully");
                    return null;
                }
            });
        }

        // ======================== Set dependencies list =========================
        private void setDependenciesList() throws IOException {
            List<NpmScope> scopes = new ArrayList<>();
            ArgumentListBuilder npmLsArgs = new ArgumentListBuilder(npmInstall.getExecutablePath(), "ls", "--json");
            if (npmInstall.getScope() == null) {
                scopes.add(NpmScope.DEVELOPMENT);
                scopes.add(NpmScope.PRODUCTION);
            } else {
                scopes.add(npmInstall.getScope());
            }
            NpmProject npmProject = new NpmProject(dependenciesClient, listener.getLogger());
            for (NpmScope scope : scopes) {
                ArgumentListBuilder scopeArguments = new ArgumentListBuilder(npmLsArgs.toCommandArray());
                scopeArguments.add("--only=" + scope);
                String npmListsResults = exe(scopeArguments);
                JsonNode jsonNode = Utils.mapper().readTree(npmListsResults);
                npmProject.addDependencies(Pair.of(scope, jsonNode));
            }

            NpmBuildInfoExtractor buildInfoExtractor = new NpmBuildInfoExtractor();
            npmInstall.setDependencies(buildInfoExtractor.extract(npmProject));
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
