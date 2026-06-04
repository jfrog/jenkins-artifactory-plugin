package org.jfrog.hudson;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;

/**
 * Persistent, dismissible warning shown in the Jenkins admin bar on every page,
 * announcing that the Artifactory plugin is deprecated and pointing users to
 * the JFrog plugin.
 *
 * Jenkins core handles dismissal via {@code AdministrativeMonitor#disable}; the
 * monitor is hidden once an admin clicks "Dismiss" in the rendered view.
 */
@Extension
public class DeprecationMonitor extends AdministrativeMonitor {

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Artifactory Plugin Deprecated";
    }
}
