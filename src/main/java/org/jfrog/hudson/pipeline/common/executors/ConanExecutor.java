package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.Vcs;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ConanExecutor implements Executor {
    private static final String CONAN_CLIENT_EXEC = "conan";
    private static final String CONAN_CONFIG_SET_CMD = CONAN_CLIENT_EXEC + " config set";
    private static final String CONAN_ADD_REMOTE_CMD = CONAN_CLIENT_EXEC + " remote add";
    private static final String CONAN_ADD_USER_CMD = CONAN_CLIENT_EXEC + " user";
    private static final String CONAN_BUILD_INFO_CMD = "conan_build_info";
    private final static String CONAN_LOG_FILE = "conan_log.log";
    private BuildInfo buildInfo;
    private String conanHome;
    private ArgumentListBuilder conanCmdArgs;
    private FilePath ws;
    private Launcher launcher;
    private TaskListener listener;
    private EnvVars env;
    private Run build;

    public ConanExecutor(String conanHome, FilePath ws, Launcher launcher, TaskListener listener, EnvVars env, Run build) {
        this(null, conanHome, ws, launcher, listener, env, build);
    }

    public ConanExecutor(BuildInfo buildInfo, String conanHome, FilePath ws, Launcher launcher, TaskListener listener, EnvVars env, Run build) {
        this.buildInfo = buildInfo;
        this.ws = ws;
        this.launcher = launcher;
        this.listener = listener;
        this.build = build;
        this.conanHome = conanHome;
        EnvVars extendedEnv = new EnvVars(env);
        extendedEnv.put(Utils.CONAN_USER_HOME, conanHome);
        this.env = extendedEnv;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public void execClientInit() throws Exception {
        this.conanCmdArgs = new ArgumentListBuilder();
        conanCmdArgs.addTokenized(CONAN_CONFIG_SET_CMD);
        // We need to add quotation marks before we save the log file path
        conanCmdArgs.add("log.trace_file=\"" + StringUtils.trim(getLogFilePath()) + "\"");
        execute();
    }

    public void execRemoteAdd(String serverName, String serverUrl, boolean force, boolean verifySSL) throws Exception {
        this.conanCmdArgs = new ArgumentListBuilder();
        conanCmdArgs.addTokenized(CONAN_ADD_REMOTE_CMD);
        if (force) {
            conanCmdArgs.add("--force");
        }
        conanCmdArgs.add(serverName);
        conanCmdArgs.add(serverUrl);
        conanCmdArgs.add(verifySSL ? "True" : "False");
        execute();
    }

    public void execUserAdd(String username, String password, String serverName) throws Exception {
        this.conanCmdArgs = new ArgumentListBuilder();
        conanCmdArgs.addTokenized(CONAN_ADD_USER_CMD);
        conanCmdArgs.add(username);
        conanCmdArgs.add("-p");
        conanCmdArgs.addMasked(password);
        conanCmdArgs.add("-r");
        conanCmdArgs.add(serverName);
        listener.getLogger().println("Adding conan user '" + username + "', server '" + serverName + "'");
        execute();
    }

    public void execCommand(String command) throws Exception {
        this.conanCmdArgs = new ArgumentListBuilder();
        conanCmdArgs.add(CONAN_CLIENT_EXEC);
        conanCmdArgs.addTokenized(command);
        buildInfo = Utils.prepareBuildinfo(build, buildInfo);
        FilePath conanHomeDirectory = new FilePath(launcher.getChannel(), conanHome);
        persistBuildProperties(conanHomeDirectory);
        execute();
        FilePath logFilePath = collectConanBuildInfo(env);
        Build regularBuildInfo = Utils.getGeneratedBuildInfo(build, listener, launcher, logFilePath.getRemote());
        buildInfo.append(regularBuildInfo);
    }

    @Override
    public void execute() throws Exception {
        try {
            if (!ws.exists()) {
                ws.mkdirs();
            }
            if (launcher.isUnix()) {
                boolean hasMaskedArguments = conanCmdArgs.hasMaskedArguments();
                StringBuilder sb = new StringBuilder();
                for (String arg : conanCmdArgs.toList()) {
                    sb.append(Utils.escapeUnixArgument(arg)).append(" ");
                }
                conanCmdArgs.clear();
                conanCmdArgs.add("sh", "-c");
                if (hasMaskedArguments) {
                    conanCmdArgs.addMasked(sb.toString());
                } else {
                    conanCmdArgs.add(sb.toString());
                }
            } else {
                conanCmdArgs = conanCmdArgs.toWindowsCommand();
            }
        } catch (Exception e) {
            listener.error("Couldn't execute the conan client executable. " + e.getMessage());
            throw new Run.RunnerAbortedException();
        }
        Utils.launch("Conan", launcher, conanCmdArgs, env, listener, ws);
    }

    private void persistBuildProperties(FilePath conanHomeDirectory) throws IOException, InterruptedException {
        FilePath buildProperties = new FilePath(conanHomeDirectory, ".conan").child("artifacts.properties");
        ;
        final long startTime = buildInfo.getStartDate().getTime();
        final Vcs vcs = Utils.extractVcs(ws, new JenkinsBuildInfoLog(listener));
        buildProperties.touch(System.currentTimeMillis());
        buildProperties.act(new PersistBuildPropertiesCallable(buildInfo, startTime, vcs));
    }

    public static class PersistBuildPropertiesCallable extends MasterToSlaveFileCallable<Boolean> {
        private static final String PROPS_PREFIX = "artifact_property_";
        private BuildInfo buildInfo;
        private long startTime;
        private Vcs vcs;

        PersistBuildPropertiesCallable(BuildInfo buildInfo, long startTime, Vcs vcs) {
            this.buildInfo = buildInfo;
            this.startTime = startTime;
            this.vcs = vcs;
        }

        public Boolean invoke(File conanProperties, VirtualChannel channel) throws IOException, InterruptedException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(conanProperties.getCanonicalFile()));) {
                pw.println(String.format("%s=%s", PROPS_PREFIX + BuildInfoFields.BUILD_NAME, buildInfo.getName()));
                pw.println(String.format("%s=%s", PROPS_PREFIX + BuildInfoFields.BUILD_NUMBER, buildInfo.getNumber()));
                pw.println(String.format("%s=%s", PROPS_PREFIX + BuildInfoFields.BUILD_TIMESTAMP, String.valueOf(startTime)));
                if (StringUtils.isNotEmpty(vcs.getRevision())) {
                    pw.println(String.format("%s=%s", PROPS_PREFIX + BuildInfoFields.VCS_REVISION, vcs.getRevision()));
                }
                if (StringUtils.isNotEmpty(vcs.getUrl())) {
                    pw.println(String.format("%s=%s", PROPS_PREFIX + BuildInfoFields.VCS_URL, vcs.getUrl()));
                }
            }
            return true;
        }
    }

    // Conan collect buildInfo as part of the tasks execution.
    // In order to transform the collected buildInfo into Artifactory buildInfo format we need to execute the conan_build_info command.
    // The conan_build_info command expect to get a path for the output file.
    private FilePath collectConanBuildInfo(EnvVars extendedEnv) throws Exception {
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        FilePath outputFilePath = tempDir.createTextTempFile("conan", "build-info", "", true);
        // We will use this executor to run conan's collect build info command
        conanCmdArgs = new ArgumentListBuilder(CONAN_BUILD_INFO_CMD, getLogFilePath(), "--output", outputFilePath.getRemote());
        execute();
        return outputFilePath;
    }

    public String getLogFilePath() {
        String separator = launcher.isUnix() ? "/" : "\\";
        if (StringUtils.endsWith(conanHome, separator)) {
            return conanHome + CONAN_LOG_FILE;
        }
        return conanHome + separator + CONAN_LOG_FILE;
    }
}
