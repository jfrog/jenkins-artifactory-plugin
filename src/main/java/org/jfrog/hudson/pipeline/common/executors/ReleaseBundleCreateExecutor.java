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
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.CreateReleaseBundleRequest;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.io.IOException;

import static org.jfrog.hudson.pipeline.common.Utils.createReleaseNotes;

public class ReleaseBundleCreateExecutor implements Executor {
    private final DistributionManagerBuilder distributionManagerBuilder;
    private final CreateReleaseBundleRequest request;
    private final transient FilePath ws;
    private final String gpgPassphrase;

    public ReleaseBundleCreateExecutor(DistributionServer server, String name, String version, String spec, String storingRepo, boolean signImmediately, boolean dryRun, String gpgPassphrase, String releaseNotesPath,
                                       String releaseNotesSyntax, String description, TaskListener listener, Run<?, ?> build, FilePath ws, EnvVars env) throws IOException {
        this.distributionManagerBuilder = server.createDistributionManagerBuilder(new JenkinsBuildInfoLog(listener), build.getParent());
        this.request = createRequest(name, version, Util.replaceMacro(spec, env), description, storingRepo, signImmediately, dryRun, releaseNotesPath, releaseNotesSyntax);
        this.ws = ws;
        this.gpgPassphrase = gpgPassphrase;
    }

    public void execute() throws IOException, InterruptedException {
        ws.act(new ReleaseBundleCreateCallable(distributionManagerBuilder, request, gpgPassphrase));
    }

    private CreateReleaseBundleRequest createRequest(String name, String version, String spec, String description,
                                                     String storingRepo, boolean signImmediately, boolean dryRun,
                                                     String releaseNotesPath, String releaseNotesSyntax) throws IOException {
        return new CreateReleaseBundleRequest.Builder(name, version)
                .releaseNotes(createReleaseNotes(releaseNotesPath, releaseNotesSyntax))
                .storingRepository(storingRepo)
                .signImmediately(signImmediately)
                .spec(spec)
                .description(description)
                .dryRun(dryRun)
                .build();
    }

    private static class ReleaseBundleCreateCallable extends MasterToSlaveFileCallable<Void> {
        private final DistributionManagerBuilder distributionManagerBuilder;
        private final CreateReleaseBundleRequest request;
        private final String gpgPassphrase;

        public ReleaseBundleCreateCallable(DistributionManagerBuilder distributionManagerBuilder, CreateReleaseBundleRequest request, String gpgPassphrase) {
            this.distributionManagerBuilder = distributionManagerBuilder;
            this.request = request;
            this.gpgPassphrase = gpgPassphrase;
        }

        @Override
        public Void invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
            try (DistributionManager distributionManager = distributionManagerBuilder.build()) {
                distributionManager.createReleaseBundle(request, gpgPassphrase);
            }
            return null;
        }
    }
}
