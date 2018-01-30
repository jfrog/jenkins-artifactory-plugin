package org.jfrog.hudson.release;

import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
        // do a dry run first
        promotion.setDryRun(true);
        listener.getLogger().println("Performing dry run promotion (no changes are made during dry run) ...");

        HttpResponse dryResponse = client.stageBuild(buildName, buildNumber, promotion);
        if (checkSuccess(dryResponse, true, promotion.isFailFast(), true, listener)) {
            listener.getLogger().println("Dry run finished successfully.\nPerforming promotion ...");
            promotion.setDryRun(false);
            HttpResponse response = client.stageBuild(buildName, buildNumber, promotion);
            if (checkSuccess(response, false, promotion.isFailFast(), true, listener)) {
                listener.getLogger().println("Promotion completed successfully!");
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the status of promotion response and return true on success
     *
     * @param response
     * @param dryRun
     * @param parseMessages
     * @param listener
     * @return
     */
    public static boolean checkSuccess(HttpResponse response, boolean dryRun, boolean failFast,
                                       boolean parseMessages, TaskListener listener) {
        StatusLine status = response.getStatusLine();
        try {
            String content = ExtractorUtils.entityToString(response.getEntity());
            if (assertResponseStatus(dryRun, failFast, listener, status, content)) {
                if (parseMessages) {
                    ExtractorUtils.validateStringNotBlank(content);
                    JSONObject json = JSONObject.fromObject(content);
                    JSONArray messages = json.getJSONArray("messages");
                    for (Object messageObj : messages) {
                        JSONObject messageJson = (JSONObject) messageObj;
                        String level = messageJson.getString("level");
                        String message = messageJson.getString("message");
                        // TODO: we don't want to fail if no items were moved/copied. find a way to support it
                        if ((level.equals("WARNING") || level.equals("ERROR")) &&
                                !message.startsWith("No items were") && failFast) {
                            listener.error("Received " + level + ": " + message);
                            return false;
                        }
                    }
                }
                listener.getLogger().println(content);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed parsing promotion response:"));
        }
        return false;
    }

    private static boolean assertResponseStatus(boolean dryRun, boolean failFast, TaskListener listener, StatusLine status, String content) {
        if (status.getStatusCode() != 200 && failFast) {
            if (dryRun) {
                listener.error(
                        "Promotion failed during dry run (no change in Artifactory was done): " + status +
                                "\n" + content);
            } else {
                listener.error(
                        "Promotion failed. View Artifactory logs for more details: " + status + "\n" + content);
            }
            return false;
        }
        return true;
    }
}
