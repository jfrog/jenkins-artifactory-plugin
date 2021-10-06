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

package org.jfrog.hudson.release.scm.svn;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.release.scm.AbstractScmCoordinator;

import java.io.IOException;

/**
 * Subversion scm coordinator. Interacts with the {@link SubversionManager} to fulfill the release process.
 *
 * @author Yossi Shaul
 */
public class SubversionCoordinator extends AbstractScmCoordinator {
    private SubversionManager scmManager;
    private boolean tagCreated;
    private final ReleaseAction releaseAction;

    public SubversionCoordinator(AbstractBuild build, BuildListener listener, ReleaseAction releaseAction) {
        super(build, listener);
        this.releaseAction = releaseAction;
    }

    public void prepare() throws IOException, InterruptedException {
        scmManager = new SubversionManager(build, listener);
    }

    public void afterSuccessfulReleaseVersionBuild() throws InterruptedException, IOException {
        if (releaseAction.isCreateVcsTag()) {
            scmManager.createTag(releaseAction.getTagUrl(), releaseAction.getTagComment());
            tagCreated = true;
        }
    }

    public void beforeDevelopmentVersionChange() {
        // nothing special for svn
    }

    @Override
    public void afterDevelopmentVersionChange(boolean modified) throws IOException, InterruptedException {
        super.afterDevelopmentVersionChange(modified);
        if (modified) {
            scmManager.commitWorkingCopy(releaseAction.getNextDevelCommitComment());
        }
    }

    public void buildCompleted() throws IOException, InterruptedException {
        if (build.getResult().isWorseThan(Result.SUCCESS)) {
            // build has failed, make sure to delete the tag and revert the working copy
            //run.getActions().remove(releaseBadge);
            scmManager.safeRevertWorkingCopy();
            if (tagCreated) {
                scmManager.safeRevertTag(releaseAction.getTagUrl(), getRevertTagMessage());
            }
        }
    }

    /**
     * @return The message to delete the tag with, if the user has inputted a custom message to commit the tag, then a
     *         <b>Reverting:</b> message will prepended to the custom message to use when deleting the tag. Otherwise,
     *         the default message will be used.
     */
    private String getRevertTagMessage() {
        String tagComment = releaseAction.getDefaultVcsConfig().getTagComment();
        if (StringUtils.equals(releaseAction.getTagComment(), tagComment)) {
            return tagComment;
        }
        return SubversionManager.COMMENT_PREFIX + "Reverting: " + releaseAction.getTagComment();
    }

    public String getRemoteUrlForPom() {
        return scmManager.getRemoteUrl(null);
    }
}
