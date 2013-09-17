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
import hudson.remoting.Which;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Maven;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.extractor.maven.Maven3BuildInfoLogger;
import org.jfrog.hudson.util.PluginDependencyHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Maven3 builder for free style projects. Hudson 1.392 added native support for maven 3 but this one is useful for free style.
 *
 * @author Yossi Shaul
 */
public class Maven3Builder extends Builder {

    public static final String CLASSWORLDS_LAUNCHER = "org.codehaus.plexus.classworlds.launcher.Launcher";

    private final String mavenName;
    private final String rootPom;
    private final String goals;
    private final String mavenOpts;

    @DataBoundConstructor
    public Maven3Builder(String mavenName, String rootPom, String goals, String mavenOpts) {
        this.mavenName = mavenName;
        this.rootPom = rootPom;
        this.goals = goals;
        this.mavenOpts = mavenOpts;
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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        FilePath workDir = build.getModuleRoot();
        ArgumentListBuilder cmdLine = buildMavenCmdLine(build, listener, env, launcher);
        String[] cmds = cmdLine.toCommandArray();
        try {
            int exitValue = launcher.launch().cmds(cmds).envs(env).stdout(listener).pwd(workDir).join();
            boolean success = (exitValue == 0);
            build.setResult(success ? Result.SUCCESS : Result.FAILURE);
            return success;
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("command execution failed"));
            build.setResult(Result.FAILURE);
            return false;
        }
    }

    private ArgumentListBuilder buildMavenCmdLine(AbstractBuild<?, ?> build, BuildListener listener,
                                                  EnvVars env, Launcher launcher) throws IOException, InterruptedException {

        Maven.MavenInstallation mi = getMaven();
        if (mi == null) {
            listener.error("Couldn't find Maven executable.");
            throw new Run.RunnerAbortedException();
        } else {
            mi = mi.forNode(Computer.currentComputer().getNode(), listener);
            mi = mi.forEnvironment(env);
        }

        FilePath mavenHome = new FilePath(launcher.getChannel(), mi.getHome());

        if (!mavenHome.exists()) {
            listener.error("Couldn't find Maven home: " + mavenHome.getRemote());
            throw new Run.RunnerAbortedException();
        }

        ArgumentListBuilder args = new ArgumentListBuilder();

        FilePath mavenBootDir = new FilePath(mavenHome, "boot");
        FilePath[] classworldsCandidates = mavenBootDir.list("plexus-classworlds*.jar");
        if (classworldsCandidates == null || classworldsCandidates.length == 0) {
            listener.error("Couldn't find classworlds jar under " + mavenBootDir.getRemote());
            throw new Run.RunnerAbortedException();
        }

        FilePath classWorldsJar = classworldsCandidates[0];

        StringBuilder javaPathBuilder = new StringBuilder();
        String jdkBinPath = env.get("PATH+JDK");
        if (StringUtils.isNotBlank(jdkBinPath)) {
            javaPathBuilder.append(jdkBinPath).append("/");
        }
        javaPathBuilder.append("java");
        if (!launcher.isUnix()) {
            javaPathBuilder.append(".exe");
        }
        args.add(javaPathBuilder.toString());

        // classpath
        args.add("-classpath");
        args.add(classWorldsJar.getRemote());

        // maven home
        args.addKeyValuePair("-D", "maven.home", mavenHome.getRemote(), false);

        String buildInfoPropertiesFile = env.get(BuildInfoConfigProperties.PROP_PROPS_FILE);
        boolean artifactoryIntegration = StringUtils.isNotBlank(buildInfoPropertiesFile);
        listener.getLogger().println("Artifactory integration is " + (artifactoryIntegration ? "enabled" : "disabled"));
        String classworldsConfPath;
        if (artifactoryIntegration) {

            args.addKeyValuePair("-D", BuildInfoConfigProperties.PROP_PROPS_FILE, buildInfoPropertiesFile, false);

            // use the classworlds conf packaged with this plugin and resolve the extractor libs
            File maven3ExtractorJar = Which.jarFile(Maven3BuildInfoLogger.class);
            FilePath actualDependencyDirectory =
                    PluginDependencyHelper.getActualDependencyDirectory(build, maven3ExtractorJar);

            if (getMavenOpts() == null || !getMavenOpts().contains("-Dm3plugin.lib")) {
                args.addKeyValuePair("-D", "m3plugin.lib", actualDependencyDirectory.getRemote(), false);
            }

            URL resource = getClass().getClassLoader().getResource("org/jfrog/hudson/maven3/classworlds-freestyle.conf");
            classworldsConfPath = copyClassWorldsFile(build, resource).getRemote();
        } else {
            classworldsConfPath = new FilePath(mavenHome, "bin/m2.conf").getRemote();
        }

        args.addKeyValuePair("-D", "classworlds.conf", classworldsConfPath, false);

        // maven opts
        if (StringUtils.isNotBlank(getMavenOpts())) {
            String mavenOpts = Util.replaceMacro(getMavenOpts(), build.getBuildVariableResolver());

            // HAP-314 - We need to separate the args, same as jenkins maven plugin does
            args.addTokenized(mavenOpts);
        }

        // classworlds launcher main class
        args.add(CLASSWORLDS_LAUNCHER);

        // pom file to build
        String rootPom = getRootPom();
        if (StringUtils.isNotBlank(rootPom)) {
            args.add("-f", rootPom);
        }

        // maven goals
        args.addTokenized(getGoals());

        return args;
    }

    /**
     * Copies a classworlds file to a temporary location either on the local filesystem or on a slave depending on the
     * node type.
     *
     * @return The path of the classworlds.conf file
     */
    private FilePath copyClassWorldsFile(AbstractBuild<?, ?> build, URL resource) {
        try {
            FilePath remoteClassworlds =
                    build.getWorkspace().createTextTempFile("classworlds", "conf", "", false);
            remoteClassworlds.copyFrom(resource);
            return remoteClassworlds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Maven.MavenInstallation getMaven() {
        Maven.MavenInstallation[] installations = getDescriptor().getInstallations();
        for (Maven.MavenInstallation i : installations) {
            if (mavenName != null && mavenName.equals(i.getName())) {
                return i;
            }
        }
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType.equals(FreeStyleProject.class);
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
            return Jenkins.getInstance().getDescriptorByType(Maven.DescriptorImpl.class).getInstallations();
        }

        @Override
        public Maven3Builder newInstance(StaplerRequest request, JSONObject formData) throws FormException {
            return (Maven3Builder) request.bindJSON(clazz, formData);
        }
    }
}
