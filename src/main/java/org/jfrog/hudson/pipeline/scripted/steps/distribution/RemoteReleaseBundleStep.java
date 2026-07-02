package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;

import java.util.List;

public abstract class RemoteReleaseBundleStep extends AbstractStepImpl {
    final DistributionServer server;
    final List<String> countryCodes;
    final String distRules;
    final String siteName;
    final String cityName;
    final boolean dryRun;
    final String version;
    final boolean sync;
    final String name;

    public RemoteReleaseBundleStep(DistributionServer server, String name, String version, boolean dryRun, boolean sync,
                                   String distRules, List<String> countryCodes, String siteName, String cityName) {
        this.server = server;
        this.name = name;
        this.version = version;
        this.dryRun = dryRun;
        this.sync = sync;
        this.distRules = distRules;
        this.countryCodes = countryCodes;
        this.siteName = siteName;
        this.cityName = cityName;
    }

    public DistributionServer getServer() {
        return server;
    }
}
