/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.maven3;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Maven;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.output.TeeOutputStream;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.PluginDependencyHelper;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Maven3 builder for free style projects. Hudson 1.392 added native support for maven 3 but this one is useful for free style.
 *
 * @author Yossi Shaul
 */
public class Maven3Builder extends Builder {

    public static final String CLASSWORLDS_LAUNCHER = "org.codehaus.plexus.classworlds.launcher.Launcher";
    public static final String MAVEN_HOME_PREFIX_IN_VERSION = "Maven home:";
    public static final String MAVEN_HOME = "MAVEN_HOME";

    private final String mavenName;
    private final String rootPom;
    private final String goals;
    private final String mavenOpts;
    private final boolean useWrapper;
    private String classworldsConfPath;

    @DataBoundConstructor
    public Maven3Builder(String mavenName, String rootPom, String goals, String mavenOpts, boolean useWrapper) {
        this.mavenName = mavenName;
        this.rootPom = rootPom;
        this.goals = goals;
        this.mavenOpts = mavenOpts;
        this.useWrapper = useWrapper;
    }

    public String getMavenName() {
        return mavenName;
    }

    public String getRootPom() {
        return rootPom;
    }

    public String getGoals() {
        return goals;
    }

    public String getMavenOpts() {
        return mavenOpts;
    }

    // Used by FreeStyle Maven jobs only
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        listener.getLogger().println("Jenkins Artifactory Plugin version: " + ActionableHelper.getArtifactoryPluginVersion());
        EnvVars env = build.getEnvironment(listener);
        FilePath workDir = build.getModuleRoot();
        FilePath ws = build.getWorkspace();
        FilePath mavenHome = getMavenHome(listener, env, launcher, ws);

        if (!mavenHome.exists()) {
            listener.error("Couldn't find Maven home: " + mavenHome.getRemote());
            throw new Run.RunnerAbortedException();
        }
        ArgumentListBuilder args = buildMavenCmdLine(build, listener, env, launcher, mavenHome, ws, ws);
        RunMaven(launcher, listener, env, workDir, args);
        return true;
    }

    // Used by Pipeline jobs only
    public void perform(Run<?, ?> build, Launcher launcher, TaskListener listener, EnvVars env, FilePath workDir, FilePath tempDir)
            throws InterruptedException, IOException {
        listener.getLogger().println("Jenkins Artifactory Plugin version: " + ActionableHelper.getArtifactoryPluginVersion());
        FilePath mavenHome = getMavenHome(listener, env, launcher, workDir);

        if (!mavenHome.exists()) {
            listener.getLogger().println("Couldn't find Maven home at " + mavenHome.getRemote() + " on agent " + Utils.getAgentName(workDir) +
                    ". This could be because this build is running inside a Docker container.");
        }
        ArgumentListBuilder args = buildMavenCmdLine(build, listener, env, launcher, mavenHome, workDir, tempDir);
        RunMaven(launcher, listener, env, workDir, args);
    }

    private void RunMaven(Launcher launcher, TaskListener listener, EnvVars env, FilePath workDir, ArgumentListBuilder args) throws IOException {
        try {
            Utils.launch("Maven", launcher, args, env, listener, workDir);
        } finally {
            ActionableHelper.deleteFilePath(workDir, classworldsConfPath);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    private ArgumentListBuilder buildMavenCmdLine(Run<?, ?> build, TaskListener listener,
                                                  EnvVars env, Launcher launcher, FilePath mavenHome, FilePath ws, FilePath tempDir)
            throws IOException, InterruptedException {

        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(Utils.getJavaPathBuilder(env.get("PATH+JDK"), launcher));

        // classpath
        String fileSeparator = getFileSeparator(launcher);
        String[] pathsToJoin = {mavenHome.getRemote(), "boot", "*"};
        args.add("-classpath", join(pathsToJoin, fileSeparator));

        // maven home
        args.addKeyValuePair("-D", "maven.home", mavenHome.getRemote(), false);

        // Default configuration should be set as system property since maven 3.5.0, for more info see: https://github.com/apache/maven/commit/be5caccaff3d00ffca4b3cefe9665b6106bc44bf?diff=split
        FilePath mavenConf = mavenHome.child("conf");
        args.addKeyValuePair("-D", "maven.conf", mavenConf.getRemote(), false);

        String buildInfoPropertiesFile = env.get(BuildInfoConfigProperties.PROP_PROPS_FILE);
        boolean artifactoryIntegration = isNotBlank(buildInfoPropertiesFile);
        listener.getLogger().println("Artifactory integration is " + (artifactoryIntegration ? "enabled" : "disabled"));
        if (artifactoryIntegration) {
            addArtifactoryIntegrationArgs(args, buildInfoPropertiesFile, tempDir, env);
            ActionableHelper.deleteFilePathOnExit(tempDir, classworldsConfPath);
        } else {
            args.addKeyValuePair("-D", "classworlds.conf", new FilePath(mavenHome, "bin/m2.conf").getRemote(), false);
        }

        //Starting from Maven 3.3.3
        args.addKeyValuePair("-D", "maven.multiModuleProjectDirectory", getMavenProjectPath(build, ws), false);

        addMavenOpts(args, build);

        // classworlds launcher main class
        args.add(CLASSWORLDS_LAUNCHER);

        // pom file to build
        String rootPom = getRootPom();
        if (isNotBlank(rootPom)) {
            args.add("-f", rootPom);
        }

        // maven goals
        args.addTokenized(Util.replaceMacro(getGoals(), env));

        return args;
    }

    private String getFileSeparator(Launcher launcher) {
        String fileSeparator = "/";
        if (!launcher.isUnix()) {
            fileSeparator = "\\";
        }
        return fileSeparator;
    }

    private void addArtifactoryIntegrationArgs(ArgumentListBuilder args, String buildInfoPropertiesFile, FilePath ws, EnvVars env) throws IOException, InterruptedException {
        args.addKeyValuePair("-D", BuildInfoConfigProperties.PROP_PROPS_FILE, buildInfoPropertiesFile, false);

        if (Boolean.parseBoolean(env.get(BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED))) {
            args.addKeyValuePair("-D", BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED, Boolean.TRUE.toString(), false);
        }

        File extractorJar = PluginDependencyHelper.getExtractorJar(env);
        FilePath actualDependencyDirectory =
                PluginDependencyHelper.getActualDependencyDirectory(extractorJar, ws);

        if (getMavenOpts() == null || !getMavenOpts().contains("-Dm3plugin.lib")) {
            args.addKeyValuePair("-D", "m3plugin.lib", actualDependencyDirectory.getRemote(), false);
        }

        URL resource = getClass().getClassLoader().getResource("org/jfrog/hudson/maven3/classworlds-freestyle.conf");
        classworldsConfPath = copyClassWorldsFile(ws, resource).getRemote();
        args.addKeyValuePair("-D", "classworlds.conf", classworldsConfPath, false);
    }

    private void addMavenOpts(ArgumentListBuilder args, Run<?, ?> build) {
        if (isNotBlank(getMavenOpts())) {
            String mavenOpts = getMavenOpts();
            if (build instanceof AbstractBuild) {
                // If we aren't in pipeline job we, might need to evaluate the variable real value.
                mavenOpts = Util.replaceMacro(getMavenOpts(), ((AbstractBuild) build).getBuildVariables());
            }
            // HAP-314 - We need to separate the args, same as jenkins maven plugin does
            args.addTokenized(mavenOpts);
        }
    }

    private FilePath getMavenHome(TaskListener listener, EnvVars env, Launcher launcher, FilePath ws) throws IOException, InterruptedException {
        String mavenHome = null;
        if (useWrapper) {
            // Use Maven wrapper
            mavenHome = createAndGetWrapper(launcher, env, listener, ws);
        } else if (isNotEmpty(mavenName)) {
            // Use Maven from tool
            mavenHome = getMavenFromTool(launcher, env, listener);
        } else if (env.get(MAVEN_HOME) != null) {
            // Use Maven from MAVEN_HOME environment variable
            mavenHome = env.get(MAVEN_HOME);
        }
        if (isBlank(mavenHome)) {
            throw new RuntimeException("Couldn't find maven installation");
        }
        return new FilePath(launcher.getChannel(), mavenHome);
    }

    /**
     * Install Maven wrapper by running `mvn -version` command and return the Maven wrapper home.
     *
     * @param launcher - Job's process launcher
     * @param env      - Job's environment variables
     * @param listener - Job's task listener
     * @param ws       - Job's workspace
     * @return Maven wrapper home
     * @throws IOException in case of any I/O error.
     */
    private String createAndGetWrapper(Launcher launcher, EnvVars env, TaskListener listener, FilePath ws) throws IOException {
        // Create output stream to parse the `mvn -version` command results
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            // Create TeeOutputStream to show the `mvn -version` command results either in the local output stream
            // and in the task listener.
            // The TeeOutputStream should NOT be closed, since it may close the task listener as well.
            TeeOutputStream printStream = new TeeOutputStream(listener.getLogger(), os);
            Utils.launch("Maven Wrapper", launcher, getMavenWrapperVersionArgs(launcher), env,
                    new StreamBuildListener(printStream), getMavenWrapperPath(ws));
            os.flush();

            // Extract the Maven wrapper home directory from the `mvn -version` output
            String mvnVersionOutput = os.toString(StandardCharsets.UTF_8.name());
            try (BufferedReader reader = new BufferedReader(new StringReader(mvnVersionOutput))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (startsWithIgnoreCase(line, MAVEN_HOME_PREFIX_IN_VERSION)) {
                        return trim(removeStartIgnoreCase(line, MAVEN_HOME_PREFIX_IN_VERSION));
                    }
                }
            }
        }

        throw new IOException("Failed to detect the Maven home of the Maven wrapper");
    }

    /**
     * Get the path to the directory containing the mvnw and mvnw.bat files.
     *
     * @param ws - Job's workspace
     * @return path to the directory containing the mvnw and mvnw.bat files
     */
    private FilePath getMavenWrapperPath(FilePath ws) {
        if (isNotBlank(rootPom)) {
            ws = ws.child(rootPom);
            if (endsWith(rootPom, ".xml")) {
                // The root pom leads directly to the pom file, but the maven wrapper should be in the pom's directory
                ws = ws.getParent();
            }
        }
        return ws;
    }

    private ArgumentListBuilder getMavenWrapperVersionArgs(Launcher launcher) {
        String wrapperExe = launcher.isUnix() ? "./mvnw" : "mvnw.cmd";
        ArgumentListBuilder args = new ArgumentListBuilder(wrapperExe, "-version");
        if (!launcher.isUnix()) {
            args = args.toWindowsCommand();
        }
        return args;
    }

    /**
     * Get Maven home from configured Maven tool.
     *
     * @param launcher - Job's process launcher
     * @param env      - Job's environment variables
     * @param listener - Job's task listener
     * @return Maven tool home
     * @throws IOException          in case of any I/O error.
     * @throws InterruptedException in case thread interrupted while trying to retrieve the running node.
     */
    private String getMavenFromTool(Launcher launcher, EnvVars env, TaskListener listener) throws IOException, InterruptedException {
        Maven.MavenInstallation mi = getMaven();
        if (mi == null) {
            listener.error("Couldn't find Maven executable.");
            throw new Run.RunnerAbortedException();
        } else {
            Node node = ActionableHelper.getNode(launcher);
            mi = mi.forNode(node, listener);
            mi = mi.forEnvironment(env);
        }

        return mi.getHome();
    }

    private Maven.MavenInstallation getMaven() {
        Maven.MavenInstallation[] installations = getDescriptor().getInstallations();
        for (Maven.MavenInstallation i : installations) {
            if (mavenName != null && mavenName.equals(i.getName())) {
                return i;
            }
        }
        return null;
    }

    private String getMavenProjectPath(Run<?, ?> build, FilePath ws) {
        if (build instanceof AbstractBuild) {
            if (isNotBlank(getRootPom())) {
                return ((AbstractBuild) build).getModuleRoot().getRemote() + File.separatorChar +
                        getRootPom().replace("/pom.xml", EMPTY);
            }
            return ((AbstractBuild) build).getModuleRoot().getRemote();
        }
        return ws.getRemote();
    }

    /**
     * Copies a classworlds file to a temporary location either on the local filesystem or on a slave depending on the
     * node type.
     *
     * @return The path of the classworlds.conf file
     */
    private FilePath copyClassWorldsFile(FilePath ws, URL resource) {
        try {
            FilePath remoteClassworlds =
                    ws.createTextTempFile("classworlds", "conf", "");
            remoteClassworlds.copyFrom(resource);
            return remoteClassworlds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return hudson.model.FreeStyleProject.class.isAssignableFrom(jobType) ||
                    hudson.matrix.MatrixProject.class.isAssignableFrom(jobType) ||
                    (Jenkins.get().getPlugin(PluginsUtils.MULTIJOB_PLUGIN_ID) != null &&
                            com.tikal.jenkins.plugins.multijob.MultiJobProject.class.isAssignableFrom(jobType));
        }

        @Override
        public String getHelpFile() {
            return "/help/project-config/maven.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.step_displayName();
        }

        public Maven.MavenInstallation[] getInstallations() {
            return Jenkins.get().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations();
        }

        @Override
        public Maven3Builder newInstance(StaplerRequest request, JSONObject formData) throws FormException {
            return (Maven3Builder) request.bindJSON(clazz, formData);
        }
    }
}
