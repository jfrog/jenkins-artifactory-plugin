package org.jfrog.hudson.pipeline.declarative.steps.distribution;

import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;

import java.util.List;

/**
 * @author yahavi
 **/
public abstract class RemoteReleaseBundleStep extends AbstractStepImpl {
    final String serverId;
    final String version;
    final String name;

    List<String> countryCodes;
    String distRules;
    String siteName;
    String cityName;
    boolean dryRun;
    boolean sync;

    RemoteReleaseBundleStep(String serverId, String name, String version) {
        this.serverId = serverId;
        this.name = name;
        this.version = version;
    }
}
