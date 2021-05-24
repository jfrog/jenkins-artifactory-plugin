package org.jfrog.hudson.release;

import hudson.model.TaskListener;
import org.jfrog.build.api.builder.DistributionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;

/**
 * Created by yahavi on 12/04/2017.
 */
public class DistributionUtils {

    /**
     * Two stage distribution, dry run and actual promotion to verify correctness.
     */
    public static boolean distributeAndCheckResponse(DistributionBuilder distributionBuilder, ArtifactoryManager artifactoryManager, TaskListener listener,
                                                     String buildName, String buildNumber, boolean dryRun) {
        // do a dry run first
        listener.getLogger().println("Performing dry run distribution (no changes are made during dry run) ...");
        if (!distribute(distributionBuilder, artifactoryManager, listener, buildName, buildNumber, true)) {
            return false;
        }
        listener.getLogger().println("Dry run finished successfully");
        if (!dryRun) {
            listener.getLogger().println("Performing distribution ...");
            if (!distribute(distributionBuilder, artifactoryManager, listener, buildName, buildNumber, false)) {
                return false;
            }
            listener.getLogger().println("Distribution completed successfully!");
        }
        return true;
    }

    private static boolean distribute(DistributionBuilder distributionBuilder, ArtifactoryManager artifactoryManager, TaskListener listener,
                                      String buildName, String buildNumber, boolean dryRun) {
        try {
            artifactoryManager.distributeBuild(buildName, buildNumber, distributionBuilder.dryRun(dryRun).build());
            return true;
        } catch (IOException e) {
            if (dryRun) {
                listener.error(
                        "Distribution failed during dry run (no change in Artifactory was done). ", e);
            } else {
                listener.error(
                        "Distribution failed. View Artifactory logs for more details. ", e);
            }
            return false;
        }
    }
}
