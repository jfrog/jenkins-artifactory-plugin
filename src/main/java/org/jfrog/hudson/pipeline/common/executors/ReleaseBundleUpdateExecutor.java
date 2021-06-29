package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.build.extractor.clientConfiguration.DistributionManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.DistributionManager;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.UpdateReleaseBundleRequest;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.io.IOException;

import static org.jfrog.hudson.pipeline.common.Utils.createReleaseNotes;

public class ReleaseBundleUpdateExecutor implements Executor {
    private final DistributionManagerBuilder distributionManagerBuilder;
    private final UpdateReleaseBundleRequest request;
    private final transient FilePath ws;
    private final String gpgPassphrase;
    private final String version;
    private final String name;

    public ReleaseBundleUpdateExecutor(DistributionServer server, String name, String version, String spec, String storingRepo,
                                       boolean signImmediately, boolean dryRun, String gpgPassphrase, String releaseNotesPath,
                                       String releaseNotesSyntax, String description, TaskListener listener, Run<?, ?> build, FilePath ws, EnvVars env) throws IOException {
        this.distributionManagerBuilder = server.createDistributionManagerBuilder(new JenkinsBuildInfoLog(listener), build.getParent());
        this.request = createRequest(Util.replaceMacro(spec, env), description, storingRepo, signImmediately, dryRun, releaseNotesPath, releaseNotesSyntax);
        this.ws = ws;
        this.gpgPassphrase = gpgPassphrase;
        this.version = version;
        this.name = name;
    }

    public void execute() throws IOException, InterruptedException {
        ws.act(new ReleaseBundleUpdateCallable(distributionManagerBuilder, request, gpgPassphrase, name, version));
    }

    private UpdateReleaseBundleRequest createRequest(String spec, String description, String storingRepo,
                                                     boolean signImmediately, boolean dryRun, String releaseNotesPath,
                                                     String releaseNotesSyntax) throws IOException {
        return new UpdateReleaseBundleRequest.Builder()
                .releaseNotes(createReleaseNotes(releaseNotesPath, releaseNotesSyntax))
                .storingRepository(storingRepo)
                .signImmediately(signImmediately)
                .spec(spec)
                .description(description)
                .dryRun(dryRun)
                .build();
    }

    private static class ReleaseBundleUpdateCallable extends MasterToSlaveFileCallable<Void> {
        private final DistributionManagerBuilder distributionManagerBuilder;
        private final UpdateReleaseBundleRequest request;
        private final String gpgPassphrase;
        private final String version;
        private final String name;

        public ReleaseBundleUpdateCallable(DistributionManagerBuilder distributionManagerBuilder, UpdateReleaseBundleRequest request, String gpgPassphrase, String name, String version) {
            this.distributionManagerBuilder = distributionManagerBuilder;
            this.request = request;
            this.gpgPassphrase = gpgPassphrase;
            this.name = name;
            this.version = version;
        }

        @Override
        public Void invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
            try (DistributionManager distributionManager = distributionManagerBuilder.build()) {
                distributionManager.updateReleaseBundle(name, version, request, gpgPassphrase);
            }
            return null;
        }
    }
}
