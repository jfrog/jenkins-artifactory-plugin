package org.jfrog.hudson.pipeline.common.executors;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.build.extractor.clientConfiguration.DistributionManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.DistributionManager;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.io.IOException;

public class ReleaseBundleSignExecutor implements Executor {
    private final DistributionManagerBuilder distributionManagerBuilder;
    private final transient FilePath ws;
    private final String gpgPassphrase;
    private final String storingRepo;
    private final String version;
    private final String name;

    public ReleaseBundleSignExecutor(DistributionServer server, String name, String version, String gpgPassphrase,
                                     String storingRepo, TaskListener listener, Run<?, ?> build, FilePath ws) {
        this.distributionManagerBuilder = server.createDistributionManagerBuilder(new JenkinsBuildInfoLog(listener), build);
        this.version = version;
        this.name = name;
        this.gpgPassphrase = gpgPassphrase;
        this.storingRepo = storingRepo;
        this.ws = ws;
    }

    public void execute() throws IOException, InterruptedException {
        ws.act(new ReleaseBundleSignCallable(distributionManagerBuilder, storingRepo, gpgPassphrase, name, version));
    }

    private static class ReleaseBundleSignCallable extends MasterToSlaveFileCallable<Void> {
        private final DistributionManagerBuilder distributionManagerBuilder;
        private final String gpgPassphrase;
        private final String storingRepo;
        private final String version;
        private final String name;

        public ReleaseBundleSignCallable(DistributionManagerBuilder distributionManagerBuilder, String storingRepo, String gpgPassphrase, String name, String version) {
            this.distributionManagerBuilder = distributionManagerBuilder;
            this.storingRepo = storingRepo;
            this.gpgPassphrase = gpgPassphrase;
            this.version = version;
            this.name = name;
        }

        @Override
        public Void invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
            try (DistributionManager distributionManager = distributionManagerBuilder.build()) {
                distributionManager.signReleaseBundle(name, version, gpgPassphrase, storingRepo);
            }
            return null;
        }
    }
}
