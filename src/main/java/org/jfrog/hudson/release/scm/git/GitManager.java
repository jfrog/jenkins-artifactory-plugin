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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.security.ACL;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jfrog.hudson.release.ReleaseRepository;
import org.jfrog.hudson.release.scm.AbstractScmManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Interacts with Git repository for the various release operations.
 *
 * @author Yossi Shaul
 */
public class GitManager extends AbstractScmManager<GitSCM> {
    private static Logger debuggingLogger = Logger.getLogger(GitManager.class.getName());
    private StandardCredentials credentials;

    public GitManager(AbstractBuild<?, ?> build, TaskListener buildListener) {
        super(build, buildListener);
    }

    public void setGitCredentials(StandardCredentials credentials) {
        this.credentials = credentials;
    }

    public void checkoutBranch(final String branch, final boolean create) throws IOException, InterruptedException {
        GitClient client = getGitClient(null);

        debuggingLogger.fine(String.format("Checkout Branch '%s' with create=%s", branch, create));
        if (create) {
            client.checkoutBranch(branch, null);
        } else {
            client.checkout(branch);
        }
    }

    public void commitWorkingCopy(final String commitMessage) throws IOException, InterruptedException {
        GitClient client = getGitClient(null);

        debuggingLogger.fine("Adding all files in the current directory");
        client.add("-u");

        debuggingLogger.fine(String.format("Committing working copy with message '%s'", commitMessage));
        client.commit(commitMessage);
    }

    public void createTag(final String tagName, final String commitMessage)
            throws IOException, InterruptedException {
        GitClient client = getGitClient(null);

        log(buildListener, String.format("Creating tag '%s' with message '%s'", tagName, commitMessage));
        client.tag(tagName, commitMessage);
    }

    public boolean isTagExists(final ReleaseRepository releaseRepository, final String tagName)
            throws IOException, InterruptedException {
        GitClient client = getGitClient(releaseRepository);

        log(buildListener, String.format("Checking if tag '%s' exists.", tagName));
        return client.tagExists(tagName);
    }

    public void testPush(final ReleaseRepository releaseRepository, final String tagName)
            throws Exception {
        // If not http/https url, skip push test
        String repositoryUrl = releaseRepository.getGitUri().toLowerCase();
        if (!repositoryUrl.startsWith("http://") && !repositoryUrl.startsWith("https://")) {
            return;
        }

        // Test push
        createTag(tagName, "this is a test tag");
        GitClient client = getGitClient(releaseRepository);
        log(buildListener, String.format("Attempting to push tag %s with --dry-run", tagName));

        List<Pair<String, StandardCredentials>> credentialsList = getGitClientCredentials();
        StandardUsernamePasswordCredentials credentials = null;
        for (Pair<String, StandardCredentials> credentialsPair : credentialsList) {
            // Look for the credentials matching ReleaseRepository
            if (credentialsPair.getKey().equals(releaseRepository.getGitUri())) {
                credentials = (StandardUsernamePasswordCredentials)credentialsPair.getValue();
                break;
            }
        }

        if (credentials == null) {
            throw new IllegalStateException("Failed to retrieve git credentials");
        }

        // Run push dry-run on the build agent
        FilePath directory = getWorkingDirectory(getJenkinsScm(), build.getWorkspace());
        directory.act(new GitPushDryRunCallable(
                credentials.getUsername(),
                credentials.getPassword().getPlainText(),
                releaseRepository.getTargetRepoPrivateUri(),
                client.getWorkTree().toURI()));
        log(buildListener,"Push dry-run completed successfully");
    }

    public void push(final ReleaseRepository releaseRepository, final String branch) throws Exception {
        GitClient client = getGitClient(releaseRepository);

        log(buildListener, String.format("Pushing branch '%s' to '%s'", branch, releaseRepository.getGitUri()));
        client.push().tags(true).to(new URIish(releaseRepository.getTargetRepoPrivateUri()))
            .ref("refs/heads/" + branch).timeout(10).execute();
    }

    public void revertWorkingCopy() throws IOException, InterruptedException {
        GitClient client = getGitClient(null);

        log(buildListener, "Reverting git working copy (reset --hard)");
        client.clean();
    }

    public void deleteLocalBranch(final String branch) throws IOException, InterruptedException {
        GitClient client = getGitClient(null);

        log(buildListener, "Deleting local git branch: " + branch);
        client.deleteBranch(branch);
    }

    public void deleteRemoteBranch(final ReleaseRepository releaseRepository, final String branch)
    throws IOException, InterruptedException {
        GitClient client = getGitClient(releaseRepository);

        log(buildListener, String.format("Deleting remote branch '%s' on '%s'", branch, releaseRepository.getGitUri()));
        client.push(releaseRepository.getRepositoryName(), ":refs/heads/" + branch);
    }

    public void deleteLocalTag(final String tag) throws IOException, InterruptedException {
        GitClient client = getGitClient(null);

        log(buildListener, "Deleting local tag: " + tag);
        client.deleteTag(tag);
    }

    public void deleteRemoteTag(final ReleaseRepository releaseRepository, final String tag)
    throws IOException, InterruptedException {
        GitClient client = getGitClient(releaseRepository);

        log(buildListener, String.format("Deleting remote tag '%s' from '%s'", tag, releaseRepository.getGitUri()));
        client.push(releaseRepository.getRepositoryName(), ":refs/tags/" + tag);
    }

    // This method is currently in use only by the SvnCoordinator
    public String getRemoteUrl(String defaultRemoteUrl) {
        if (StringUtils.isBlank(defaultRemoteUrl)) {
            RemoteConfig remoteConfig = getJenkinsScm().getRepositories().get(0);
            URIish uri = remoteConfig.getURIs().get(0);
            return uri.toPrivateString();
        }

        return defaultRemoteUrl;
    }

    public String getBranchNameWithoutRemote(String branchName) {
        List<RemoteConfig> repositories = getJenkinsScm().getRepositories();
        for (RemoteConfig remoteConfig : repositories) {
            String prefix = remoteConfig.getName() + "/";
            if (branchName.startsWith(prefix)) {
                return StringUtils.removeStart(branchName, prefix);
            }
        }
        return branchName;
    }

    public ReleaseRepository getRemoteConfig(String defaultRemoteNameOrUrl) throws IOException {
        List<RemoteConfig> repositories = getJenkinsScm().getRepositories();
        if (StringUtils.isBlank(defaultRemoteNameOrUrl)) {
            if (repositories == null || repositories.isEmpty()) {
                throw new GitException("Git remote config repositories are null or empty.");
            }
            return new ReleaseRepository(repositories.get(0).getURIs().get(0), repositories.get(0).getName());
        }

        for (RemoteConfig remoteConfig : repositories) {
            if (remoteConfig.getName().equals(defaultRemoteNameOrUrl)) {
                return new ReleaseRepository(remoteConfig.getURIs().get(0), remoteConfig.getName());
            }
        }

        if (checkGitValidUri(defaultRemoteNameOrUrl)) {
            return new ReleaseRepository(defaultRemoteNameOrUrl, "externalGitUrl");
        }

        throw new IOException("Target Remote Name: " + defaultRemoteNameOrUrl + " ,doesn`t exist");
    }

    private boolean checkGitValidUri(String defaultRemoteNameOrUrl) {
        String regex = "(\\w+://)(.+@)*([\\w\\d\\.]+)(:[\\d]+){0,1}/*(.*)|(.+@)*([\\w\\d\\.]+):(.*)|file://(.*)";
        return Pattern.compile(regex).matcher(defaultRemoteNameOrUrl).matches();
    }

    private GitClient getGitClient(ReleaseRepository releaseRepository) throws IOException, InterruptedException {
        FilePath directory = getWorkingDirectory(getJenkinsScm(), build.getWorkspace());
        EnvVars env = build.getEnvironment(buildListener);

        Git git = new Git(buildListener, env);
        git.in(directory);

        /*
        * When init the git exe, the user dons`t have to add SSH credentials in the git plugin.
        *  This solution automatically takes the user default SSH ($HOME/.ssh)
        * */
        git.using(getJenkinsScm().getGitExe(build.getBuiltOn(), buildListener)); // git.exe
        GitClient client = git.getClient();

        client.setCommitter(StringUtils.defaultIfEmpty(env.get("GIT_COMMITTER_NAME"), ""),
                StringUtils.defaultIfEmpty(env.get("GIT_COMMITTER_EMAIL"), ""));
        client.setAuthor(StringUtils.defaultIfEmpty(env.get("GIT_AUTHOR_NAME"), ""),
                StringUtils.defaultIfEmpty(env.get("GIT_AUTHOR_EMAIL"), ""));


        if (releaseRepository != null && releaseRepository.isTargetRepoUri()) {
            client.setRemoteUrl(releaseRepository.getRepositoryName(), releaseRepository.getTargetRepoPrivateUri());
        } else {
            addRemoteRepoToConfig(client);
        }
        addCredentialsToGitClient(client);

        return client;
    }

    private String getFirstGitURI(RemoteConfig remoteRepository) {
        List<URIish> urIs = remoteRepository.getURIs();
        if (urIs == null || urIs.isEmpty()) {
            throw new GitException("Error performing push tag command, repository URIs are null or empty.");
        }

        return urIs.get(0).toString();
    }

    /*
    * In cause the remote repository is not exists in the git config file
    * */
    private void addRemoteRepoToConfig(GitClient client) throws InterruptedException {
        GitSCM gitScm = getJenkinsScm();
        for (RemoteConfig uc : gitScm.getRepositories()) {
            if (client.getRemoteUrl(uc.getName()) == null)
                client.setRemoteUrl(uc.getName(), uc.getURIs().get(0).toPrivateASCIIString());
        }
    }

    private void addCredentialsToGitClient(GitClient client) {
        List<Pair<String, StandardCredentials>> credentialsList = getGitClientCredentials();

        for (Pair<String, StandardCredentials> entry : credentialsList) {
            client.addCredentials(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns a List of Pair<key, value> which contains the git credentials to use.
     * Key - url to repository
     * Value - StandardCredentials, containing username and password
     */
    private List<Pair<String, StandardCredentials>> getGitClientCredentials() {
        List<Pair<String, StandardCredentials>> credentialsList = new ArrayList<>();
        GitSCM gitScm = getJenkinsScm();
        for (UserRemoteConfig uc : gitScm.getUserRemoteConfigs()) {
            String url = uc.getUrl();
            // In case overriding credentials are defined, we will use it for this URL
            if (this.credentials != null) {
                credentialsList.add(Pair.of(url, this.credentials));
                continue;
            }

            // Get credentials from jenkins credentials plugin
            if (uc.getCredentialsId() != null) {
                StandardUsernameCredentials credentials = CredentialsMatchers.firstOrNull(
                                CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class,
                                        build.getProject(), ACL.SYSTEM, URIRequirementBuilder.fromUri(url).build()),
                                CredentialsMatchers.allOf(CredentialsMatchers.withId(uc.getCredentialsId()),
                                        GitClient.CREDENTIALS_MATCHER));
                if (credentials != null) {
                    credentialsList.add(Pair.of(url, (StandardCredentials)credentials));
                }
            }
        }
        return credentialsList;
    }

    private FilePath getWorkingDirectory(GitSCM gitSCM, FilePath ws) throws IOException {
        // working directory might be relative to the workspace
        String relativeTargetDir = gitSCM.getRelativeTargetDir() == null ? "" : gitSCM.getRelativeTargetDir();
        return new FilePath(ws, relativeTargetDir);
    }
}
