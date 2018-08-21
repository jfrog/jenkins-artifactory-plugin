package org.jfrog.hudson.release;

import hudson.model.TaskListener;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.util.ExtractorUtils;

import java.io.IOException;

/**
 * Created by romang on 6/21/16.
 */
public class PromotionUtils {

    /**
     * Two stage promotion, dry run and actual promotion to verify correctness.
     *
     * @param promotion
     * @param client
     * @param listener
     * @param buildName
     * @param buildNumber
     * @throws IOException
     */
    public static boolean promoteAndCheckResponse(Promotion promotion, ArtifactoryBuildInfoClient client, TaskListener listener,
                                                  String buildName, String buildNumber) throws IOException {
        // If failFast is true, perform dry run first
        if (promotion.isFailFast()) {
            promotion.setDryRun(true);
            listener.getLogger().println("Performing dry run promotion (no changes are made during dry run) ...");
            HttpResponse dryResponse = client.stageBuild(buildName, buildNumber, promotion);
            try {
                validatePromotionSuccessful(dryResponse, true, promotion.isFailFast(), listener);
            } catch (IOException e) {
                listener.error(e.getMessage());
                return false;
            }
            listener.getLogger().println("Dry run finished successfully.\nPerforming promotion ...");
        }

        // Perform promotion
        promotion.setDryRun(false);
        HttpResponse response = client.stageBuild(buildName, buildNumber, promotion);
        try {
            validatePromotionSuccessful(response, false, promotion.isFailFast(), listener);
        } catch (IOException e) {
            listener.error(e.getMessage());
            return false;
        }
        listener.getLogger().println("Promotion completed successfully!");

        return true;
    }

    /**
     * Checks the status of promotion response and return true on success
     *
     * @param response
     * @param dryRun
     * @param failFast
     * @param listener
     * @return
     */
    public static void validatePromotionSuccessful(HttpResponse response, boolean dryRun, boolean failFast, TaskListener listener) throws IOException {
        StatusLine status = response.getStatusLine();
        String content;

        try {
            content = ExtractorUtils.entityToString(response.getEntity());
        } catch (IOException e) {
            throw new IOException("Failed parsing promotion response.", e);
        }

        if (failOnResponseStatus(dryRun, failFast, listener, status, content)) {
            throw new IOException("Promotion failed due to Artifactory response.");
        }

        listener.getLogger().println(content);
    }

    private static boolean failOnResponseStatus(boolean dryRun, boolean failFast, TaskListener listener, StatusLine status, String content) {
        if (status.getStatusCode() != 200 && failFast) {
            if (dryRun) {
                listener.error(
                        "Promotion failed during dry run (no change in Artifactory was done): " + status +
                                "\n" + content);
            } else {
                listener.error(
                        "Promotion failed. View Artifactory logs for more details: " + status + "\n" + content);
            }
            return true;
        }
        return false;
    }
}
