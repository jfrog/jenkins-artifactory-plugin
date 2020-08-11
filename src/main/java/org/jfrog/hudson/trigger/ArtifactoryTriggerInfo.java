package org.jfrog.hudson.trigger;

import hudson.model.InvisibleAction;
import org.jfrog.hudson.ArtifactoryServer;

/**
 * Use this job action to save the Artifactory server configured in pipeline jobs.
 *
 * @author yahavi
 */
public class ArtifactoryTriggerInfo extends InvisibleAction {

    private final ArtifactoryServer server;

    public ArtifactoryTriggerInfo(ArtifactoryServer server) {
        this.server = server;
    }

    public ArtifactoryServer getServer() {
        return server;
    }
}
