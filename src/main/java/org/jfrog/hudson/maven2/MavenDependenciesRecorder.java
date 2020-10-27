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

package org.jfrog.hudson.maven2;

import hudson.Extension;
import hudson.maven.*;
import hudson.maven.MavenBuildProxy.BuildCallable;
import hudson.model.BuildListener;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.jfrog.hudson.MavenDependenciesRecord;
import org.jfrog.hudson.MavenDependency;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Records dependencies used during the build.
 *
 * @author Yossi Shaul
 */
public class MavenDependenciesRecorder extends MavenReporter {

    /**
     * All dependencies this module used, including transitive ones.
     */
    private transient Set<MavenDependency> dependencies;

    @Override
    public boolean preBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) {
        listener.getLogger().println("[HUDSON] Collecting dependencies info");
        dependencies = new HashSet<MavenDependency>();
        return true;
    }

    /**
     * Mojos perform different dependency resolution, so we add dependencies for each mojo.
     */
    @Override
    public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener,
            Throwable error) {
        //listener.getLogger().println("[MavenDependenciesRecorder] mojo: " + mojo.getClass() + ":" + mojo.getGoal());
        //listener.getLogger().println("[MavenDependenciesRecorder] dependencies: " + pom.getArtifacts());
        recordMavenDependencies(pom.getArtifacts());
        return true;
    }

    /**
     * Sends the collected dependencies over to the master and record them.
     */
    @Override
    public boolean postBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener)
            throws InterruptedException, IOException {
        build.executeAsync(new BuildCallable<Void, IOException>() {
            // record is transient, so needs to make a copy first
            private final Set<MavenDependency> d = dependencies;

            public Void call(MavenBuild build) throws IOException, InterruptedException {
                // add the action
                //TODO: [by yl] These actions are persisted into the build.xml of each build run - we need another
                //context to store these actions
                build.addAction(new MavenDependenciesRecord(build, d));
                return null;
            }
        });
        return true;
    }

    private void recordMavenDependencies(Set<Artifact> artifacts) {
        if (artifacts != null) {
            for (Artifact dependency : artifacts) {
                if (dependency.isResolved() && dependency.getFile() != null) {
                    dependencies.add(new MavenDependency(dependency));
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends MavenReporterDescriptor {
        @Override
        public String getDisplayName() {
            return "Record Maven Dependencies";
        }

        @Override
        public MavenReporter newAutoInstance(MavenModule module) {
            return new MavenDependenciesRecorder();
        }
    }

    private static final long serialVersionUID = 1L;
}
