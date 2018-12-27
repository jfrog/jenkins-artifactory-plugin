/*
 * Copyright (C) 2012 JFrog Ltd.
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

package org.jfrog.hudson.release.scm.perforce;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import org.jfrog.build.vcs.perforce.PerforceClient;
import org.jfrog.hudson.release.scm.AbstractScmManager;

import java.io.File;
import java.io.IOException;

/**
 * Performs Perforce actions for the release management.
 *
 * @author Yossi Shaul
 */
public abstract class AbstractPerforceManager<T extends SCM> extends AbstractScmManager<T> {

    protected PerforceClient perforce;

    public AbstractPerforceManager(AbstractBuild<?, ?> build, TaskListener buildListener) {
        super(build, buildListener);
    }

    public abstract void prepare() throws IOException, InterruptedException;

    public void commitWorkingCopy(int changeListId, String commitMessage) throws Exception {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new IOException("Workspace is null, cannot commit changes");
        }
        establishConnection().commitWorkingCopy(changeListId, commitMessage);
    }

    public void createTag(String label, String commitMessage, String changeListId) throws IOException {
        perforce.createLabel(label, commitMessage, changeListId);
    }

    public void revertWorkingCopy(int changeListId) throws IOException {
        perforce.revertWorkingCopy(changeListId);
    }

    public void deleteLabel(String tagUrl) throws IOException {
        perforce.deleteLabel(tagUrl);
    }

    /**
     * Creates a new changelist and returns its id number
     *
     * @return The id of the newly created changelist
     * @throws IOException In case of errors communicating with perforce server
     */
    public int createNewChangeList() throws IOException {
        return perforce.createNewChangeList();
    }

    public int getDefaultChangeListId() throws IOException {
        return perforce.getDefaultChangeListId();
    }

    public void commitWorkingCopy(String commitMessage) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Use the overloaded method");
    }

    public void createTag(String tagUrl, String commitMessage) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Use the overloaded method");
    }

    public String getRemoteUrl(String defaultRemoteUrl) {
        throw new UnsupportedOperationException("Remote URL not supported");
    }

    /**
     * Opens file for editing.
     *
     * @param currentChangeListId The current change list id to open the file for editing at
     * @param filePath            The filePath which contains the file we need to edit
     * @throws IOException          Thrown in case of perforce communication errors
     * @throws InterruptedException
     */
    public void edit(int currentChangeListId, FilePath filePath) throws Exception {
        establishConnection().editFile(currentChangeListId, new File(filePath.getRemote()));
    }

    /**
     * PerforceClient is using one-time server which closes connection after each operation
     * This method will re-establish a client-server connection
     *
     * @return PerforceClient instance that established a one time connection to the server
     * @throws Exception
     */
    public abstract PerforceClient establishConnection() throws Exception;

    public void closeConnection() throws IOException {
        perforce.closeConnection();
    }

}
