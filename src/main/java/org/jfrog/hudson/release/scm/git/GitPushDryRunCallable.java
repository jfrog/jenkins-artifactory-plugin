package org.jfrog.hudson.release.scm.git;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Created by Bar Belity on 30/04/2018.
 */
public class GitPushDryRunCallable extends MasterToSlaveFileCallable<Void> {
    private String username;
    private String password;
    private String targetUri;
    private URI uri;

    public GitPushDryRunCallable(String username, String password, String targetUri, URI uri) throws IOException, InterruptedException {
        this.username = username;
        this.password = password;
        this.targetUri = targetUri;
        this.uri = uri;
    }

    @Override
    public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        org.eclipse.jgit.transport.CredentialsProvider provider =
                new UsernamePasswordCredentialsProvider(username, password);
        org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.open(new File(uri));
        try {
            PushCommand pc = git.push().setRemote(targetUri)
                    .setCredentialsProvider(provider)
                    .setDryRun(true)
                    .setPushTags();

            pc.call();
        } catch(GitAPIException e) {
            throw new IOException(e);
        } finally {
            if (git.getRepository() != null) {
                git.getRepository().close();
            }
        }
        return null;
    }
}
