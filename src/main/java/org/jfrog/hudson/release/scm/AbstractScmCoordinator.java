/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.release.scm;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.scm.SCM;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.release.scm.git.GitCoordinator;
import org.jfrog.hudson.release.scm.perforce.P4Manager;
import org.jfrog.hudson.release.scm.perforce.PerforceCoordinator;
import org.jfrog.hudson.release.scm.svn.SubversionCoordinator;

import java.io.IOException;

/**
 * This class coordinates between the release steps and required scm actions based on the svm manager.
 *
 * @author Yossi Shaul
 */
public abstract class AbstractScmCoordinator implements ScmCoordinator {

    protected final AbstractBuild build;
    protected final BuildListener listener;
    protected boolean modifiedFilesForReleaseVersion;
    protected boolean modifiedFilesForDevVersion;

    public AbstractScmCoordinator(AbstractBuild build, BuildListener listener) {
        this.build = build;
        this.listener = listener;
    }

    public static ScmCoordinator createScmCoordinator(AbstractBuild build, BuildListener listener,
                                                      ReleaseAction releaseAction) {
        SCM projectScm = build.getProject().getScm();
        if (isSvn(build.getProject())) {
            return new SubversionCoordinator(build, listener, releaseAction);
        }
        // Git is optional SCM so we cannot use the class here
        if (isGitScm(build.getProject())) {
            return new GitCoordinator(build, listener, releaseAction);
        }
        // Perforce is optional SCM so we cannot use the class here
        if (isP4SCM(build.getProject())){
            return new PerforceCoordinator(build, listener, releaseAction, new P4Manager(build, listener));
        }
        throw new UnsupportedOperationException(
                "Scm of type: " + projectScm.getClass().getName() + " is not supported");
    }

    private static boolean isP4SCM(AbstractProject project) {
        SCM projectSCM = project.getScm();
        return projectSCM != null && projectSCM.getClass().getName().equals("org.jenkinsci.plugins.p4.PerforceScm");
    }

    public static boolean isSvn(AbstractProject project) {
        SCM scm = project.getScm();
        if (scm != null) {
            return scm.getClass().getName().equals("hudson.scm.SubversionSCM");
        }
        return false;
    }

    public static boolean isGitScm(AbstractProject project) {
        SCM scm = project.getScm();
        if (scm != null) {
            return scm.getClass().getName().equals("hudson.plugins.git.GitSCM");
        }
        return false;
    }

    protected void log(String message) {
        listener.getLogger().println("[RELEASE] " + message);
    }

    public void beforeReleaseVersionChange() throws IOException, InterruptedException {

    }

    public void afterReleaseVersionChange(boolean modified) throws IOException, InterruptedException {
        modifiedFilesForReleaseVersion = modified;
    }

    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        modifiedFilesForDevVersion = modified;
    }

    public void edit(FilePath filePath) throws IOException, InterruptedException {

    }
}
