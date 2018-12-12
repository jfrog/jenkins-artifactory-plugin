package org.jfrog.hudson.release.maven;

import hudson.maven.MavenModuleSet;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * This class is used for managing the Artifactory Release Staging functionality for Maven projects using the Artifactory
 * Release Staging API.
 * The API is invoked using a URL with the following pattern:
 * <Jenkins server>/Jenkins>/job/<Project>/artifactory/staging?<Arguments List>
 */
public class MavenReleaseApiAction extends BaseMavenReleaseAction {
    public MavenReleaseApiAction(MavenModuleSet project) {
        super(project);
    }

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return "artifactory";
    }

    /**
     * This method is used to initiate a release staging process using the API.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void doStaging(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        doApi(req, resp);
    }
}
