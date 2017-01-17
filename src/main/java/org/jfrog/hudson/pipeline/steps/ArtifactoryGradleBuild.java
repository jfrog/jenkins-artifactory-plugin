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
import org.jfrog.build.api.Build;
import org.jfrog.hudson.gradle.GradleInitScriptWriter;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.MavenGradleEnvExtractor;
import org.jfrog.hudson.pipeline.types.GradleBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.types.deployers.GradleDeployer;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class ArtifactoryGradleBuild extends AbstractStepImpl {

    private GradleBuild gradleBuild;
    private String tasks;
    private String buildFile;
    private String rootDir;
    private String tool;
    private String switches;
    private BuildInfo buildInfo;
    private boolean useWrapper;
    private boolean usesPlugin;

    @DataBoundConstructor
    public ArtifactoryGradleBuild(GradleBuild gradleBuild, String tool, String rootDir, String buildFile, String tasks, String switches, boolean useWrapper, BuildInfo buildInfo, boolean usesPlugin) {
        this.gradleBuild = gradleBuild;
        this.tasks = tasks == null ? "artifactoryPublish" : tasks;
        this.rootDir = rootDir == null ? "" : rootDir;
        this.buildFile = StringUtils.isEmpty(buildFile) ? "build.gradle" : buildFile;
        this.tool = tool == null ? "" : tool;
        this.switches = switches == null ? "" : switches;
        this.buildInfo = buildInfo;
        this.useWrapper = useWrapper;
        this.usesPlugin = usesPlugin;
    }

    public GradleBuild getGradleBuild() {
        return gradleBuild;
    }

    public String getTool() {
        return tool;
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

    public void setGradleBuild(GradleBuild gradleBuild) {
        this.gradleBuild = gradleBuild;
    }

    public boolean isUseWrapper() {
        return useWrapper;
    }

    public boolean isUsesPlugin() {
        return usesPlugin;
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
        private transient ArtifactoryGradleBuild step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected BuildInfo run() throws Exception {
            BuildInfo buildInfo = step.getBuildInfo() == null ? new BuildInfo(build) : step.getBuildInfo();
            Deployer deployer = getDeployer();
            deployer.createPublisherBuildInfoDetails(buildInfo);
            MavenGradleEnvExtractor envExtractor = new MavenGradleEnvExtractor(build,
                    buildInfo, deployer, step.getGradleBuild().getResolver(), listener, launcher);
            ArgumentListBuilder args = getGradleExecutor();
            envExtractor.buildEnvVars(ws, env);
            exe(args);
            Build regularBuildInfo = Utils.getGeneratedBuildInfo(build, env, listener, ws, launcher);
            buildInfo.append(regularBuildInfo);
            return buildInfo;
        }

        private Deployer getDeployer() {
            Deployer deployer = step.getGradleBuild().getDeployer();
            if (deployer == null || deployer.isEmpty()) {
                deployer = GradleDeployer.EMPTY_DEPLOYER;
            }
            return deployer;
        }

        private ArgumentListBuilder getGradleExecutor() {
            ArgumentListBuilder args = new ArgumentListBuilder();
            if (step.isUseWrapper()) {
                String execName = !launcher.isUnix() ? GradleInstallation.WINDOWS_GRADLE_WRAPPER_COMMAND : GradleInstallation.UNIX_GRADLE_WRAPPER_COMMAND;
                FilePath gradleWrapperFile = new FilePath(new FilePath(ws, step.getRootDir()), execName);
                args.add(gradleWrapperFile.getRemote());
            } else {
                try {
                    getGradleHome(args);
                } catch (IOException e) {
                    listener.error("Couldn't find Gradle executable.");
                    build.setResult(Result.FAILURE);
                    throw new Run.RunnerAbortedException();
                } catch (InterruptedException e) {
                    listener.error("Couldn't find Gradle executable.");
                    build.setResult(Result.FAILURE);
                    throw new Run.RunnerAbortedException();
                }
            }
            args.addTokenized(getSwitches());
            args.addTokenized(getTasks());
            args.add("-b");
            args.add(step.getBuildFile());
            if (!launcher.isUnix()) {
                args = args.toWindowsCommand();
            }
            return args;
        }

        private String getTasks() {
            String tasks = step.getTasks();
            if (!(tasks.contains("artifactoryP") || tasks.contains("artifactoryPublish"))) {
                tasks += " artifactoryPublish";
            }
            return tasks;
        }

        private String getSwitches() {
            String switches = step.getSwitches();
            if (!step.getGradleBuild().isUsesPlugin()) {
                try {
                    switches += " --init-script " + createInitScript();
                } catch (Exception e) {
                    listener.getLogger().println("Error occurred while writing Gradle Init Script: " + e.getMessage());
                    build.setResult(Result.FAILURE);
                }
            }
            return switches;
        }

        private void exe(ArgumentListBuilder args) {
            StringBuilder pwd = new StringBuilder(ws.getRemote())
                    .append(launcher.isUnix() ? "/" : "\\")
                    .append(step.getRootDir());
            boolean failed;
            try {
                int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(pwd.toString()).join();
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
            if (!StringUtils.isEmpty(step.getTool())) {
                GradleInstallation[] installations = Jenkins.getInstance().getDescriptorByType(Gradle.DescriptorImpl.class).getInstallations();
                for (GradleInstallation i : installations) {
                    if (step.getTool().equals(i.getName())) {
                        return i;
                    }
                }
            }
            return null;
        }

        private FilePath getGradleHome(ArgumentListBuilder args) throws IOException, InterruptedException {
            if (!StringUtils.isEmpty(step.getTool())) {
                GradleInstallation gi = getGradleInstallation();
                if (gi == null) {
                    listener.error("Couldn't find Gradle executable.");
                    build.setResult(Result.FAILURE);
                    throw new Run.RunnerAbortedException();
                } else {
                    Node node = Utils.getNode(launcher);
                    gi = gi.forNode(node, listener);
                    gi = gi.forEnvironment(env);
                }

                env.put("GRADLE_HOME", gi.getHome());
                args.add(gi.getExecutable(launcher));
                return new FilePath(launcher.getChannel(), gi.getHome());
            }

            if (env.get("GRADLE_HOME") != null) {
                return new FilePath(new File(env.get("GRADLE_HOME")));
            }
            build.setResult(Result.FAILURE);
            throw new RuntimeException("Couldn't find gradle installation");
        }

        private String createInitScript() throws Exception {
            GradleInitScriptWriter writer = new GradleInitScriptWriter(build, launcher);
            FilePath initScript = ws.createTextTempFile("init-artifactory", "gradle",
                    writer.generateInitScript(), false);
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
