package org.jfrog.hudson.BintrayPublish;

import hudson.model.*;
import org.jfrog.hudson.*;
import java.util.List;

/**
 * This Class and Action have been deprecated.
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
