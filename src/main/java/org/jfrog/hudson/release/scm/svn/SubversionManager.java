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

package org.jfrog.hudson.release.scm.svn;

import hudson.FilePath;
import hudson.maven.agent.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.scm.SubversionSCM;
import org.jfrog.hudson.release.scm.AbstractScmManager;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs commit operations on Subversion repository configured for the project.
 *
 * @author Yossi Shaul
 */
public class SubversionManager extends AbstractScmManager<SubversionSCM> {
    private static Logger debuggingLogger = Logger.getLogger(SubversionManager.class.getName());

    public SubversionManager(AbstractBuild<?, ?> abstractBuild, TaskListener buildListener) {
        super(abstractBuild, buildListener);
    }

    public void prepare() {
        // nothing to do here
    }

    /**
     * Commits the working copy.
     *
     * @param commitMessage@return The commit info upon successful operation.
     * @throws IOException On IO of SVN failure
     */
    public void commitWorkingCopy(final String commitMessage) throws IOException, InterruptedException {
        build.getWorkspace().act(new SVNCommitWorkingCopyCallable(commitMessage, getLocation(),
                getSvnAuthenticationProvider(), buildListener));
    }

    /**
     * Creates a tag directly from the working copy.
     *
     * @param tagUrl        The URL of the tag to create.
     * @param commitMessage Commit message
     * @return The commit info upon successful operation.
     * @throws IOException On IO of SVN failure
     */
    public void createTag(final String tagUrl, final String commitMessage)
            throws IOException, InterruptedException {
        build.getWorkspace()
                .act(new SVNCreateTagCallable(tagUrl, commitMessage, getLocation(), getSvnAuthenticationProvider(),
                        buildListener));
    }

    /**
     * Revert all the working copy changes.
     */
    public void revertWorkingCopy() throws IOException, InterruptedException {
        build.getWorkspace()
                .act(new RevertWorkingCopyCallable(getLocation(), getSvnAuthenticationProvider(), buildListener));
    }

    /**
     * Attempts to revert the working copy. In case of failure it just logs the error.
     */
    public void safeRevertWorkingCopy() {
        try {
            revertWorkingCopy();
        } catch (Exception e) {
            debuggingLogger.log(Level.FINE, "Failed to revert working copy", e);
            log("Failed to revert working copy: " + e.getLocalizedMessage());
            Throwable cause = e.getCause();
            if (!(cause instanceof SVNException)) {
                return;
            }
            SVNException svnException = (SVNException) cause;
            if (svnException.getErrorMessage().getErrorCode() == SVNErrorCode.WC_LOCKED) {
                // work space locked attempt cleanup and try to revert again
                try {
                    cleanupWorkingCopy();
                } catch (Exception unlockException) {
                    debuggingLogger.log(Level.FINE, "Failed to cleanup working copy", e);
                    log("Failed to cleanup working copy: " + e.getLocalizedMessage());
                    return;
                }

                try {
                    revertWorkingCopy();
                } catch (Exception revertException) {
                    log("Failed to revert working copy on the 2nd attempt: " + e.getLocalizedMessage());
                }
            }
        }
    }

    private void cleanupWorkingCopy() throws IOException, InterruptedException {
        build.getWorkspace().act(new CleanupCallable(getLocation(), getSvnAuthenticationProvider(), buildListener));
    }

    public void safeRevertTag(String tagUrl, String commitMessageSuffix) {
        try {
            log("Reverting subversion tag: " + tagUrl);
            SVNURL svnUrl = SVNURL.parseURIEncoded(tagUrl);
            SVNCommitClient commitClient = new SVNCommitClient(createAuthenticationManager(), null);
            SVNCommitInfo commitInfo = commitClient.doDelete(new SVNURL[]{svnUrl},
                    COMMENT_PREFIX + commitMessageSuffix);
            SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
            if (errorMessage != null) {
                log("Failed to revert '" + tagUrl + "': " + errorMessage.getFullMessage());
            }
        } catch (SVNException e) {
            log("Failed to revert '" + tagUrl + "': " + e.getLocalizedMessage());
        }
    }

    public String getRemoteUrl() {
        return getLocation().remote;
    }

    public SubversionSCM.ModuleLocation getLocation() {
        return getJenkinsScm().getLocations()[0];
    }

    private ISVNAuthenticationManager createAuthenticationManager() {
        ISVNAuthenticationProvider sap = getSvnAuthenticationProvider();
        ISVNAuthenticationManager sam = SVNWCUtil.createDefaultAuthenticationManager();
        sam.setAuthenticationProvider(sap);
        return sam;
    }

    private ISVNAuthenticationProvider getSvnAuthenticationProvider() {
        ISVNAuthenticationProvider sap = getJenkinsScm().getDescriptor().createAuthenticationProvider();
        if (sap == null) {
            throw new AbortException("Subversion authentication info is not set.");
        }
        return sap;
    }

    private static class SVNCommitWorkingCopyCallable implements FilePath.FileCallable<Object> {
        private final String commitMessage;
        private final SubversionSCM.ModuleLocation location;
        private final ISVNAuthenticationProvider authProvider;
        private final TaskListener buildListener;

        public SVNCommitWorkingCopyCallable(String commitMessage, SubversionSCM.ModuleLocation location,
                ISVNAuthenticationProvider provider, TaskListener listener) {
            this.commitMessage = commitMessage;
            this.location = location;
            authProvider = provider;
            buildListener = listener;
        }

        public Object invoke(File ws, VirtualChannel channel) throws IOException, InterruptedException {
            File workingCopy = new File(ws, location.getLocalDir()).getCanonicalFile();
            try {
                ISVNAuthenticationManager sam = SVNWCUtil.createDefaultAuthenticationManager();
                sam.setAuthenticationProvider(authProvider);
                SVNCommitClient commitClient = new SVNCommitClient(sam, null);
                buildListener.getLogger().println("[RELEASE] " + commitMessage);
                debuggingLogger.fine(String.format("Committing working copy: '%s'", workingCopy));
                SVNCommitInfo commitInfo = commitClient.doCommit(new File[]{workingCopy}, true,
                        commitMessage, null, null, true, true, SVNDepth.INFINITY);
                SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
                if (errorMessage != null) {
                    throw new IOException("Failed to commit working copy: " + errorMessage.getFullMessage());
                }
                return null;
            } catch (SVNException e) {
                throw new IOException(e.getMessage());
            }
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Creates a tag directly from the working copy.
     */
    private static class SVNCreateTagCallable implements FilePath.FileCallable<Object> {
        private final String tagUrl;
        private final String commitMessage;
        private final SubversionSCM.ModuleLocation location;
        private final ISVNAuthenticationProvider authProvider;
        private final TaskListener buildListener;

        public SVNCreateTagCallable(String tagUrl, String commitMessage, SubversionSCM.ModuleLocation location,
                ISVNAuthenticationProvider provider, TaskListener listener) {
            this.tagUrl = tagUrl;
            this.commitMessage = commitMessage;
            this.location = location;
            authProvider = provider;
            buildListener = listener;
        }

        public Object invoke(File ws, VirtualChannel channel) throws IOException, InterruptedException {
            File workingCopy = new File(ws, location.getLocalDir()).getCanonicalFile();
            try {
                SVNURL svnUrl = SVNURL.parseURIEncoded(tagUrl);

                SVNCopyClient copyClient;
                try {
                    copyClient = SubversionSCM.createClientManager(authProvider).getCopyClient();
                } catch (NoSuchMethodError e) {
                    //todo remove when backward compatibility not needed
                    //fallback for older versions of org.jenkins-ci.plugins:subversion
                    buildListener.getLogger().println(
                            "[RELEASE] You are using an old subversion jenkins plugin, please consider upgrading.");
                    copyClient = SubversionSCM.createSvnClientManager(authProvider).getCopyClient();
                }

                buildListener.getLogger().println("[RELEASE] Creating subversion tag: " + tagUrl);
                SVNCopySource source = new SVNCopySource(SVNRevision.WORKING, SVNRevision.WORKING, workingCopy);
                SVNCommitInfo commitInfo = copyClient.doCopy(new SVNCopySource[]{source},
                        svnUrl, false, true, true, commitMessage, new SVNProperties());
                SVNErrorMessage errorMessage = commitInfo.getErrorMessage();
                if (errorMessage != null) {
                    throw new IOException("Failed to create tag: " + errorMessage.getFullMessage());
                }
                return null;
            } catch (SVNException e) {
                throw new IOException("Subversion tag creation failed: " + e.getMessage());
            }
        }
    }

    private static class RevertWorkingCopyCallable implements FilePath.FileCallable<Object> {
        private final SubversionSCM.ModuleLocation location;
        private final ISVNAuthenticationProvider authProvider;
        private final TaskListener listener;

        public RevertWorkingCopyCallable(SubversionSCM.ModuleLocation location, ISVNAuthenticationProvider authProvider,
                TaskListener listener) {
            this.location = location;
            this.authProvider = authProvider;
            this.listener = listener;
        }


        public Object invoke(File ws, VirtualChannel channel) throws IOException, InterruptedException {
            File workingCopy = new File(ws, location.getLocalDir()).getCanonicalFile();
            try {
                log(listener, "Reverting working copy: " + workingCopy);
                ISVNAuthenticationManager sam = SVNWCUtil.createDefaultAuthenticationManager();
                sam.setAuthenticationProvider(authProvider);
                SVNWCClient wcClient = new SVNWCClient(sam, null);
                wcClient.doRevert(new File[]{workingCopy}, SVNDepth.INFINITY, null);
                return null;
            } catch (SVNException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    private static class CleanupCallable implements FilePath.FileCallable<Object> {
        private final SubversionSCM.ModuleLocation location;
        private final ISVNAuthenticationProvider authProvider;
        private final TaskListener listener;

        private CleanupCallable(SubversionSCM.ModuleLocation location, ISVNAuthenticationProvider authProvider,
                TaskListener listener) {
            this.location = location;
            this.authProvider = authProvider;
            this.listener = listener;
        }

        public Object invoke(File ws, VirtualChannel channel) throws IOException, InterruptedException {
            File workingCopy = new File(ws, location.getLocalDir()).getCanonicalFile();
            try {
                log(listener, "Cleanup working copy: " + workingCopy);
                ISVNAuthenticationManager sam = SVNWCUtil.createDefaultAuthenticationManager();
                sam.setAuthenticationProvider(authProvider);
                SVNWCClient wcClient = new SVNWCClient(sam, null);
                wcClient.doCleanup(workingCopy);
                return null;
            } catch (SVNException e) {
                throw new IOException(e.getMessage());
            }
        }
    }
}
