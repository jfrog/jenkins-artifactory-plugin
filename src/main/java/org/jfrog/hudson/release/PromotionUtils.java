package org.jfrog.hudson.release;

import hudson.model.TaskListener;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;

/**
 * Created by romang on 6/21/16.
 */
public class PromotionUtils {

    /**
     * Two stage promotion, dry run and actual promotion to verify correctness.
     *
     */
    public static boolean promoteAndCheckResponse(Promotion promotion, ArtifactoryManager artifactoryManager, TaskListener listener,
                                                  String buildName, String buildNumber) {
        // If failFast is true, perform dry run first
        if (promotion.isFailFast()) {
            promotion.setDryRun(true);
            listener.getLogger().println("Performing dry run promotion (no changes are made during dry run) ...");

            try {
                artifactoryManager.stageBuild(buildName, buildNumber, promotion);
                listener.getLogger().println("Dry run finished successfully.\nPerforming promotion ...");
            } catch (IOException e) {
                if (onPromotionFailFast(true, promotion.isFailFast(), listener)) {
                    return false;
                }
                listener.getLogger().println("Dry run failed to perform promotion ...");
            }
        }

        // Perform promotion
        promotion.setDryRun(false);
        try {
            artifactoryManager.stageBuild(buildName, buildNumber, promotion);
        } catch (IOException e) {
            listener.error(e.getMessage());
            return false;
        }
        listener.getLogger().println("Promotion completed successfully!");

        return true;
    }

    public static boolean onPromotionFailFast(boolean dryRun, boolean failFast, TaskListener listener) {
        if (failFast) {
            if (dryRun) {
                listener.error(
                        "Promotion failed during dry run (no change in Artifactory was done).");
            } else {
                listener.error(
                        "Promotion failed. View Artifactory logs for more details.");
            }
            return true;
        }
        return false;
    }
}
