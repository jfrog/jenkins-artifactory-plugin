package org.jfrog.hudson.trigger;

import hudson.model.Cause;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryCause extends Cause {

    private String url;

    public ArtifactoryCause(String url) {
        this.url = url;
    }

    @Override
    public String getShortDescription() {
        return "The build was triggered by Artifactory trigger." +
                "\nThe artifact that triggered the build is: " + url;
    }
}