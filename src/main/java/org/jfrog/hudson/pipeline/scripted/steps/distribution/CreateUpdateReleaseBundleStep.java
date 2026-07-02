package org.jfrog.hudson.pipeline.scripted.steps.distribution;

import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jfrog.hudson.pipeline.common.types.DistributionServer;

public abstract class CreateUpdateReleaseBundleStep extends AbstractStepImpl {
    final DistributionServer server;
    final String releaseNotesSyntax;
    final boolean signImmediately;
    final String releaseNotesPath;
    final String gpgPassphrase;
    final String storingRepo;
    final String description;
    final boolean dryRun;
    final String version;
    final String name;
    final String spec;

    public CreateUpdateReleaseBundleStep(DistributionServer server, String name, String version, String spec,
                                         String storingRepo, boolean signImmediately, boolean dryRun,
                                         String gpgPassphrase, String releaseNotesPath, String releaseNotesSyntax,
                                         String description) {
        this.server = server;
        this.name = name;
        this.version = version;
        this.spec = spec;
        this.storingRepo = storingRepo;
        this.signImmediately = signImmediately;
        this.dryRun = dryRun;
        this.gpgPassphrase = gpgPassphrase;
        this.releaseNotesPath = releaseNotesPath;
        this.releaseNotesSyntax = releaseNotesSyntax;
        this.description = description;
    }

    public String getSpec() {
        return spec;
    }

    public DistributionServer getServer() {
        return server;
    }
}
