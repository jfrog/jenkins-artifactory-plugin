package org.jfrog.hudson.pipeline.scripted.steps.conan;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import hudson.util.IOUtils;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.api.Build;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class RunCommandStep extends AbstractStepImpl {
    private String command;
    private String conanHome;
    private String buildLogPath;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public RunCommandStep(String command, String conanHome, String buildLogPath, BuildInfo buildInfo) {
        this.command = command;
        this.conanHome = conanHome;
        this.buildInfo = buildInfo;
        this.buildLogPath = buildLogPath;
    }

    public String getCommand() {
        return this.command;
    }

    public String getConanHome() {
        return this.conanHome;
    }

    public BuildInfo getBuildInfo() {
        return this.buildInfo;
    }

    public String getBuildLogPath() {
        return this.buildLogPath;
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
        private transient RunCommandStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected BuildInfo run() throws Exception {
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            FilePath conanHomeDirectory = new FilePath(launcher.getChannel(), step.getConanHome());
            persistBuildProperties(buildInfo, conanHomeDirectory);
            EnvVars extendedEnv = new EnvVars(env);
            extendedEnv.put(Utils.CONAN_USER_HOME, step.getConanHome());
            execConanCommand(extendedEnv);
            FilePath logFilePath = execConanCollectBuildInfo(extendedEnv);
            Build regularBuildInfo = Utils.getGeneratedBuildInfo(build, listener, launcher, logFilePath.getRemote());
            buildInfo.append(regularBuildInfo);
            return buildInfo;
        }

        // Conan collect buildInfo as part of the tasks execution.
        // In order to transform the collected buildInfo into Artifctory buildInfo format we need to execute the conan_build_info command.
        // The conan_build_info command expect to get a path for the output file.
        private FilePath execConanCollectBuildInfo(EnvVars extendedEnv) throws Exception {
            FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
            FilePath logFilePath = tempDir.createTextTempFile("conan", "build-info", "", true);
            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add("conan_build_info");
            args.add(step.getBuildLogPath());
            args.add("--output");
            args.add(logFilePath.getRemote());
            Utils.exeConan(args, ws, launcher, listener, build, extendedEnv);
            return logFilePath;
        }

        private void execConanCommand(EnvVars extendedEnv) {
            ArgumentListBuilder args = new ArgumentListBuilder();
            args.add("conan");
            args.addTokenized(step.getCommand());
            Utils.exeConan(args, ws, launcher, listener, build, extendedEnv);
        }

        private void persistBuildProperties(BuildInfo buildInfo, FilePath conanHomeDirectory) throws IOException, InterruptedException {
            FilePath buildProperties = new FilePath(conanHomeDirectory, ".conan").child("artifacts.properties");
            final String buildName = buildInfo.getName();
            final String buildNumber = buildInfo.getNumber();
            final String revision = Utils.extractVcsRevision(ws);
            final long startTime = buildInfo.getStartDate().getTime();
            buildProperties.touch(0);
            buildProperties.act(new MasterToSlaveFileCallable<Boolean>() {
                public Boolean invoke(File conanProperties, VirtualChannel channel) throws IOException, InterruptedException {
                    final String propsPrefix = "artifact_property_";
                    Properties props = new Properties();
                    props.setProperty(propsPrefix + "build.name", buildName);
                    props.setProperty(propsPrefix + "build.number", buildNumber);
                    props.setProperty(propsPrefix + "build.timestamp", String.valueOf(startTime));
                    if (StringUtils.isNotEmpty(revision)) {
                        props.setProperty(propsPrefix + "vcs.revision", revision);
                    }
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(conanProperties.getCanonicalFile());
                        props.store(fos, "Build properties");
                    } finally {
                        IOUtils.closeQuietly(fos);
                    }
                    return true;
                }
            });
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(RunCommandStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "runConanCommand";
        }

        @Override
        public String getDisplayName() {
            return "Run a Conan command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}