/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.hudson.util;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.maven.MavenEmbedderException;
import hudson.maven.MavenEmbedderUtils;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.tasks.Maven;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jfrog.hudson.maven3.MavenVersionCallable;

import java.io.File;
import java.io.IOException;

/**
 * Utility class that provides methods that are related to Maven version checks.
 *
 * @author Tomer Cohen
 */
public class MavenVersionHelper {
    /**
     * Minimal Maven version that works with {@link AbstractRepositoryListener}
     */
    private static final String MINIMUM_MAVEN_VERSION = "3.0.2";


    public static boolean isLowerThanMaven3(MavenModuleSetBuild build, EnvVars vars, BuildListener listener)
            throws IOException, InterruptedException {
        String version = getMavenVersion(build, vars, listener);
        if (StringUtils.isBlank(version)) {
            return true;
        }
        ComparableVersion foundVersion = new ComparableVersion(version);
        ComparableVersion neededVersion = new ComparableVersion(MINIMUM_MAVEN_VERSION);
        return foundVersion.compareTo(neededVersion) < 0;
    }

    public static boolean isLowerThanMaven3(MavenModuleSetBuild build) {
        ComparableVersion foundVersion = new ComparableVersion(build.getMavenVersionUsed());
        ComparableVersion neededVersion = new ComparableVersion(MINIMUM_MAVEN_VERSION);

        return foundVersion.compareTo(neededVersion) < 0;
    }

    /**
     * @return True if the Maven version of this build is at least {@link MavenVersionHelper#MINIMUM_MAVEN_VERSION}
     */
    public static boolean isAtLeastResolutionCapableVersion(MavenModuleSetBuild build, EnvVars vars,
            TaskListener listener) throws IOException, InterruptedException {
        return isAtLeastVersion(build, vars, listener, MINIMUM_MAVEN_VERSION);
    }

    /**
     * @return True if the Maven version is at least the version that is passed.
     */
    public static boolean isAtLeastVersion(MavenModuleSetBuild build, EnvVars vars, TaskListener listener,
            String version) throws IOException, InterruptedException {
        Maven.MavenInstallation mavenInstallation = getMavenInstallation(build, vars, listener);
        return isAtLeast(build, mavenInstallation.getHome(), version);
    }

    /**
     * Get the {@link hudson.model.EnvironmentSpecific} and {@link hudson.slaves.NodeSpecific} Maven installation. First
     * get the descriptor from the global Jenkins. Then populate it accordingly from the specific environment node that
     * the process is currently running in e.g. the MAVEN_HOME variable may be defined only in the remote node and
     * Jenkins is not persisting it as part of its installations.
     *
     * @param build  The Maven project build that the maven installation is taken from.
     * @param vars     The build's environment variables.
     * @param listener The build's event listener
     * @throws hudson.AbortException If the {@link hudson.tasks.Maven.MavenInstallation} that is taken from the project
     *                               is {@code null} then this exception is thrown.
     */
    private static Maven.MavenInstallation getMavenInstallation(MavenModuleSetBuild build, EnvVars vars,
            TaskListener listener) throws IOException, InterruptedException {
        MavenModuleSet project = build.getProject();
        Maven.MavenInstallation mavenInstallation = project.getMaven();
        if (mavenInstallation == null) {
            throw new AbortException("A Maven installation needs to be available for this project to be built.\n" +
                    "Either your server has no Maven installations defined, or the requested Maven version does not exist.");
        }
        return mavenInstallation.forEnvironment(vars).forNode(build.getBuiltOn().toComputer().getNode(), listener);
    }

    private static boolean isAtLeast(MavenModuleSetBuild build, String mavenHome, String version)
            throws IOException, InterruptedException {
        return build.getWorkspace().act(new MavenVersionCallable(mavenHome, version));
    }

    private static String getMavenVersion(MavenModuleSetBuild build, EnvVars vars,
            BuildListener listener) throws IOException, InterruptedException {
        final Maven.MavenInstallation installation = getMavenInstallation(build, vars, listener);
        final String home = installation.getHome();
        if (build.getWorkspace() == null) {
            throw new IllegalStateException("build.getWorkspace() is null");
        }
        return build.getWorkspace().act(new MasterToSlaveCallable<String, IOException>() {
             @Override
             public String call() throws IOException {
                try {
                    return MavenEmbedderUtils.getMavenVersion(new File(home)).getVersion();
                } catch (MavenEmbedderException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
