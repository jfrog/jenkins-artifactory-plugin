package org.jfrog.hudson.BintrayPublish;

import hudson.model.AbstractBuild;
import org.jfrog.hudson.BuildInfoAwareConfigurator;

import java.util.List;

/**
 * This Class and Action has been deprecated.
 * The deprecated class remains in code because it appears in config.xml of jobs that were created before the action has
 * been officially removed.
 */
@Deprecated
public class BintrayPublishAction {
    private String MINIMAL_SUPPORTED_VERSION = null;
    private AbstractBuild build = null;
    private BuildInfoAwareConfigurator configurator = null;
    private Boolean override = null;
    private String subject = null;
    private String repoName = null;
    private String packageName = null;
    private String versionName = null;
    private String signMethod = null;
    private List<String> licenses = null;
    private String passphrase = null;
    private String vcsUrl = null;
}
