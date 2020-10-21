package org.jfrog.hudson.trigger;

import hudson.model.Cause;
import org.kohsuke.stapler.export.Exported;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryCause extends Cause {

    private final String url;

    public ArtifactoryCause(String url) {
        this.url = url;
    }

    @Exported(visibility = 3)
    public String getUrl() {
        return url;
    }

    @Override
    public String getShortDescription() {
        return "The build was triggered by a change in Artifactory." +
                "\nThe artifact which triggered the build is: " + url;
    }
}