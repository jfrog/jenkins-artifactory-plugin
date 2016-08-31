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

package org.jfrog.hudson.release.scm.git;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.release.scm.AbstractScmCoordinator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Git scm coordinator. Interacts with the {@link GitManager} to fulfill the release process.
 *
 * @author Yossi Shaul
 */
public class GitCoordinator extends AbstractScmCoordinator {
    private static Logger debuggingLogger = Logger.getLogger(GitCoordinator.class.getName());

    private final ReleaseAction releaseAction;
    private GitManager scmManager;
    private String releaseBranch;
    private String checkoutBranch;

    private State state = new State();

    public GitCoordinator(AbstractBuild build, BuildListener listener, ReleaseAction releaseAction) {
        super(build, listener);
        this.releaseAction = releaseAction;
    }

    public void prepare() throws IOException, InterruptedException {
        releaseBranch = releaseAction.getReleaseBranch();

        // find the current local built branch
        String gitBranchName = build.getEnvironment(listener).get("GIT_BRANCH");
        checkoutBranch = StringUtils.substringAfter(gitBranchName, "/");

        scmManager = new GitManager(build, listener);
        scmManager.setGitCredentials(releaseAction.getGitCredentials());
    }

    public void beforeReleaseVersionChange() throws IOException, InterruptedException {
        if (releaseAction.isCreateReleaseBranch()) {
            // create a new branch for the release and start it
            scmManager.checkoutBranch(releaseBranch, true);
            state.currentWorkingBranch = releaseBranch;
            state.releaseBranchCreated = true;
        } else {
            // make sure we are on the checkout branch
            scmManager.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;
        }
        state.initialGitRevision = scmManager.revParse(state.currentWorkingBranch);
    }

    public void afterReleaseVersionChange(boolean modified) throws IOException, InterruptedException {
        super.afterReleaseVersionChange(modified);
        if (modifiedFilesForReleaseVersion) {
            // commit local changes immediately - so that e.g. if the build uses it's own git commit, that commit
            // correctly points to the release commit. In general, it's good to avoid "dirty" builds.
            log(String.format("Committing release version on branch '%s'", state.currentWorkingBranch));
            scmManager.commitWorkingCopy(releaseAction.getTagComment());
        }

        if (releaseAction.isCreateVcsTag()) {
            // create tag
            log(String.format("Committing release tag '%s' on branch '%s'", releaseAction.getTagUrl(), state.currentWorkingBranch));
            scmManager.createTag(releaseAction.getTagUrl(), releaseAction.getTagComment());
            state.tagCreated = true;
        }
    }

    public void afterSuccessfulReleaseVersionBuild() throws Exception {
        // nop
    }

    public void beforeDevelopmentVersionChange() throws IOException, InterruptedException {
        if (releaseAction.isCreateReleaseBranch()) {
            // done working on the release branch, checkout back to master
            scmManager.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;
        }
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        super.afterDevelopmentVersionChange(modified);
        if (modified) {
            log(String.format("Committing next development version on branch '%s'", state.currentWorkingBranch));
            scmManager.commitWorkingCopy(releaseAction.getNextDevelCommitComment());
        }
    }

    public void buildCompleted() throws Exception {
        if (build.getResult().isBetterOrEqualTo(Result.SUCCESS)) {
            // pull before attempting to push changes?
            //scmManager.pull(scmManager.getRemoteUrl(), checkoutBranch);

            // only push at the very end - this avoid unnecessary cleanup and makes the build as atomic as possible
            // from the point of view of the git repo.
            if (state.releaseBranchCreated) {
                log(String.format("Pushing release branch '%s'", releaseBranch));
                scmManager.push(scmManager.getRemoteConfig(releaseAction.getTargetRemoteName()), releaseBranch);
            }
            log(String.format("Pushing branch '%s'", checkoutBranch));
            scmManager.push(scmManager.getRemoteConfig(releaseAction.getTargetRemoteName()), checkoutBranch);
        } else {
            // go back to the original checkout branch (required to delete the release branch and reset the working copy)
            scmManager.checkoutBranch(checkoutBranch, false);
            state.currentWorkingBranch = checkoutBranch;

            if (state.releaseBranchCreated) {
                safeDeleteBranch(releaseBranch);
            }
            if (state.tagCreated) {
                safeDeleteTag(releaseAction.getTagUrl());
            }
            // reset changes done on the original checkout branch (next dev version)
            safeRevertWorkingCopy(checkoutBranch, state.initialGitRevision);
        }
    }

    private void safeDeleteBranch(String branch) {
        try {
            scmManager.deleteLocalBranch(branch);
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to delete release branch: ", e);
            log("Failed to delete release branch: " + e.getLocalizedMessage());
        }
    }

    private void safeDeleteTag(String tag) {
        try {
            scmManager.deleteLocalTag(tag);
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to delete tag: ", e);
            log("Failed to delete tag: " + e.getLocalizedMessage());
        }
    }

    private void safeRevertWorkingCopy(String branch, ObjectId revision) {
        try {
            scmManager.revertWorkingCopyTo(branch, revision.name());
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to revert working copy: ", e);
            log("Failed to revert working copy: " + e.getLocalizedMessage());
        }
    }

    public String getRemoteUrlForPom() {
        return null;
    }

    private static class State {
        ObjectId initialGitRevision;
        String currentWorkingBranch;
        boolean releaseBranchCreated;
        boolean tagCreated;
    }
}
