package org.jfrog.hudson.pipeline.declarative.steps.distribution;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author yahavi
 **/
public abstract class CreateUpdateReleaseBundleStep extends AbstractStepImpl {
    final String serverId;
    final String version;
    final String name;
    final String spec;

    String releaseNotesSyntax;
    boolean signImmediately;
    String releaseNotesPath;
    String gpgPassphrase;
    String storingRepo;
    String description;
    String specPath;
    boolean dryRun;

    public CreateUpdateReleaseBundleStep(String serverId, String name, String version, String spec) {
        this.serverId = serverId;
        this.name = name;
        this.version = version;
        this.spec = spec;
    }

    static String getSpec(String specPath, String specParameter) throws IOException {
        if (StringUtils.isNotBlank(specPath)) {
            return FileUtils.readFileToString(new File(specPath), StandardCharsets.UTF_8);
        }
        return specParameter;
    }
}