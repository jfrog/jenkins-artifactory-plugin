package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.gradle.Gradle;
import hudson.plugins.gradle.GradleInstallation;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.gradle.GradleInitScriptWriter;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.MavenGradleEnvExtractor;
import org.jfrog.hudson.pipeline.types.GradleBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class ArtifactoryGradleBuild extends AbstractStepImpl {

    private GradleBuild gradleBuild;
    private String tasks;
    private String buildFile;
    private String rootDir;
    private String switches;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public ArtifactoryGradleBuild(GradleBuild gradleBuild, String rootDir, String buildFile, String tasks, String switches, BuildInfo buildInfo) {
        this.gradleBuild = gradleBuild;
        this.tasks = tasks == null ? "artifactoryPublish" : tasks;
        this.rootDir = rootDir == null ? "" : rootDir;
        this.buildFile = StringUtils.isEmpty(buildFile) ? "build.gradle" : buildFile;
        this.switches = switches == null ? "" : switches;
        this.buildInfo = buildInfo;
    }

    public GradleBuild getGradleBuild() {
        return gradleBuild;
    }

    public String getSwitches() {
        return switches;
    }

    public String getTasks() {
        return tasks;
    }

    public String getBuildFile() {
        return buildFile;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public String getRootDir() {
        return rootDir;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;
        private String initScriptPath;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient ArtifactoryGradleBuild step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        private transient EnvVars extendedEnv;

        private transient FilePath tempDir;

        @Override
        protected BuildInfo run() throws Exception {
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            Deployer deployer = step.getGradleBuild().getDeployer();
            deployer.createPublisherBuildInfoDetails(buildInfo);
            String revision = Utils.extractVcsRevision(new FilePath(ws, step.getRootDir()));
            extendedEnv = new EnvVars(env);
            extendedEnv.put(ExtractorUtils.GIT_COMMIT, revision);
            MavenGradleEnvExtractor envExtractor = new MavenGradleEnvExtractor(build,
                    buildInfo, deployer, step.getGradleBuild().getResolver(), listener, launcher);
            tempDir = ExtractorUtils.createAndGetTempDir(launcher, ws);
            ArgumentListBuilder args = getGradleExecutor();
            envExtractor.buildEnvVars(tempDir, extendedEnv);
            exe(args);
            String generatedBuildPath = extendedEnv.get(BuildInfoFields.GENERATED_BUILD_INFO);
            buildInfo.append(Utils.getGeneratedBuildInfo(build, listener, launcher, generatedBuildPath));
            ActionableHelper.deleteFilePath(tempDir, initScriptPath);
            // Read the deployable artifacts list from the 'json' file in the agent and append them to the buildInfo object.
            buildInfo.appendDeployableArtifacts(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS), tempDir, listener);
            buildInfo.setAgentName(Utils.getAgentName(ws));
            return buildInfo;
        }

        private ArgumentListBuilder getGradleExecutor() {
            ArgumentListBuilder args = new ArgumentListBuilder();
            if (step.getGradleBuild().isUseWrapper()) {
                String execName = launcher.isUnix() ? GradleInstallation.UNIX_GRADLE_WRAPPER_COMMAND : GradleInstallation.WINDOWS_GRADLE_WRAPPER_COMMAND;
                FilePath gradleWrapperFile = new FilePath(new FilePath(ws, step.getRootDir()), execName);
                args.add(gradleWrapperFile.getRemote());
            } else {
                try {
                    args.add(getGradleExe());
                } catch (Exception e) {
                    listener.error("Couldn't find Gradle executable.");
                    build.setResult(Result.FAILURE);
                    throw new Run.RunnerAbortedException();
                }
            }
            args.addTokenized(getSwitches());
            args.addTokenized(step.getTasks());
            args.add("-b");
            args.add(getBuildFileFullPath());
            if (!launcher.isUnix()) {
                args = args.toWindowsCommand();
            }
            return args;
        }

        private String getBuildFileFullPath() {
            StringBuilder buildFile = new StringBuilder();
            if (StringUtils.isNotEmpty(step.getRootDir())) {
                String pathsDelimiter = launcher.isUnix() ? "/" : "\\";
                buildFile.append(step.getRootDir());
                if (!StringUtils.endsWith(step.getRootDir(), pathsDelimiter)) {
                    buildFile.append(pathsDelimiter);
                }
            }
            buildFile.append(step.getBuildFile());
            return buildFile.toString();
        }

        private String getSwitches() {
            String switches = step.getSwitches();
            if (!step.getGradleBuild().isUsesPlugin()) {
                try {
                    initScriptPath = createInitScript();
                    switches += " --init-script " + initScriptPath;
                } catch (Exception e) {
                    listener.getLogger().println("Error occurred while writing Gradle Init Script: " + e.getMessage());
                    build.setResult(Result.FAILURE);
                }
            }
            return switches;
        }

        private void exe(ArgumentListBuilder args) {
            boolean failed;
            try {
                int exitValue = launcher.launch().cmds(args).envs(extendedEnv).stdout(listener).pwd(ws).join();
                failed = (exitValue != 0);
            } catch (Exception e) {
                listener.error("Couldn't execute gradle task. " + e.getMessage());
                build.setResult(Result.FAILURE);
                throw new Run.RunnerAbortedException();
            }
            if (failed) {
                build.setResult(Result.FAILURE);
                throw new Run.RunnerAbortedException();
            }
        }

        private GradleInstallation getGradleInstallation() {
            if (!StringUtils.isEmpty(step.getGradleBuild().getTool())) {
                GradleInstallation[] installations = Jenkins.getInstance().getDescriptorByType(Gradle.DescriptorImpl.class).getInstallations();
                for (GradleInstallation i : installations) {
                    if (step.getGradleBuild().getTool().equals(i.getName())) {
                        return i;
                    }
                }
            }
            return null;
        }

        private String getGradleExe() throws IOException, InterruptedException {
            if (StringUtils.isNotEmpty(step.getGradleBuild().getTool())) {
                GradleInstallation gi = getGradleInstallation();
                if (gi == null) {
                    listener.error("Couldn't find Gradle executable.");
                    throw new Run.RunnerAbortedException();
                } else {
                    Node node = ActionableHelper.getNode(launcher);
                    gi = gi.forNode(node, listener);
                    gi = gi.forEnvironment(extendedEnv);
                }

                extendedEnv.put("GRADLE_HOME", gi.getHome());
                String gradleExe = gi.getExecutable(launcher);
                if (gradleExe != null) {
                    return gradleExe;
                }
            }
            if (!extendedEnv.containsKey("GRADLE_HOME")) {
                throw new RuntimeException("Couldn't find gradle installation");
            }
            return extendedEnv.get("GRADLE_HOME") + "/bin/gradle";
        }

        private String createInitScript() throws Exception {
            GradleInitScriptWriter writer = new GradleInitScriptWriter(tempDir);
            FilePath initScript = tempDir.createTextTempFile("init-artifactory", "gradle", writer.generateInitScript());
            ActionableHelper.deleteFilePathOnExit(initScript);
            String initScriptPath = initScript.getRemote();
            initScriptPath = initScriptPath.replace('\\', '/');
            return initScriptPath;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ArtifactoryGradleBuild.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "ArtifactoryGradleBuild";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory gradle";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
