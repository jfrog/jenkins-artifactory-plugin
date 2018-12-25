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
        return "The build was triggered by a change in Artifactory." +
                "\nThe artifact which triggered the build is: " + url;
    }
}