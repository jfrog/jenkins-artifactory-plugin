package org.jfrog.hudson.pipeline.common.executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.DistributionManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.DistributionManager;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DistributeReleaseBundleRequest;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.jfrog.hudson.util.SerializationUtils.createMapper;

public class ReleaseBundleDistributeExecutor implements Executor {
    private final DistributionManagerBuilder distributionManagerBuilder;
    private final DistributeReleaseBundleRequest request;
    private final transient FilePath ws;
    private final String version;
    private final boolean sync;
    private final String name;

    public ReleaseBundleDistributeExecutor(DistributionServer server, String name, String version, boolean dryRun, boolean sync,
                                           String distRules, List<String> countryCodes, String siteName,
                                           String cityName, TaskListener listener, Run<?, ?> build, FilePath ws) throws IOException {
        this.distributionManagerBuilder = server.createDistributionManagerBuilder(new JenkinsBuildInfoLog(listener), build.getParent());
        this.request = createRequest(distRules, countryCodes, siteName, cityName, dryRun);
        this.ws = ws;
        this.name = name;
        this.version = version;
        this.sync = sync;
    }

    public void execute() throws IOException, InterruptedException {
        ws.act(new ReleaseBundleDistributeCallable(distributionManagerBuilder, request, name, version, sync));
    }

    private DistributeReleaseBundleRequest createRequest(String distRules, List<String> countryCodes,
                                                         String siteName, String cityName, boolean dryRun) throws IOException {
        DistributeReleaseBundleRequest request;
        if (StringUtils.isNotBlank(distRules)) {
            if (!CollectionUtils.isEmpty(countryCodes) || !StringUtils.isAllBlank(siteName, cityName)) {
                throw new IOException("The distRules input can't be used with site, city or country codes");
            }
            ObjectMapper mapper = createMapper();
            request = mapper.readValue(distRules, DistributeReleaseBundleRequest.class);
        } else {
            request = new DistributeReleaseBundleRequest();
            request.setDistributionRules(Utils.createDistributionRules(countryCodes, siteName, cityName));
        }
        request.setDryRun(dryRun);
        return request;
    }

    private static class ReleaseBundleDistributeCallable extends MasterToSlaveFileCallable<Void> {
        private final DistributionManagerBuilder distributionManagerBuilder;
        private final DistributeReleaseBundleRequest request;
        private final String version;
        private final boolean sync;
        private final String name;

        public ReleaseBundleDistributeCallable(DistributionManagerBuilder distributionManagerBuilder, DistributeReleaseBundleRequest request, String name, String version, boolean sync) {
            this.distributionManagerBuilder = distributionManagerBuilder;
            this.request = request;
            this.name = name;
            this.version = version;
            this.sync = sync;
        }

        @Override
        public Void invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
            try (DistributionManager distributionManager = distributionManagerBuilder.build()) {
                distributionManager.distributeReleaseBundle(name, version, sync, request);
            }
            return null;
        }
    }
}
