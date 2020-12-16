package org.jfrog.hudson.release;

import hudson.model.TaskListener;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.builder.DistributionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.util.ExtractorUtils;

import java.io.IOException;

/**
 * Created by yahavi on 12/04/2017.
 */
public class DistributionUtils {

    /**
     * Two stage distribution, dry run and actual promotion to verify correctness.
     *
     * @param distributionBuilder
     * @param client
     * @param listener
     * @param buildName
     * @param buildNumber
     * @throws IOException
     */
    public static boolean distributeAndCheckResponse(DistributionBuilder distributionBuilder, ArtifactoryBuildInfoClient client, TaskListener listener,
                                                     String buildName, String buildNumber, boolean dryRun) throws IOException {
        // do a dry run first
        listener.getLogger().println("Performing dry run distribution (no changes are made during dry run) ...");
        if (!distribute(distributionBuilder, client, listener, buildName, buildNumber, true)) {
            return false;
        }
        listener.getLogger().println("Dry run finished successfully");
        if (!dryRun) {
            listener.getLogger().println("Performing distribution ...");
            if (!distribute(distributionBuilder, client, listener, buildName, buildNumber, false)) {
                return false;
            }
            listener.getLogger().println("Distribution completed successfully!");
        }
        return true;
    }

    private static boolean distribute(DistributionBuilder distributionBuilder, ArtifactoryBuildInfoClient client, TaskListener listener,
                                      String buildName, String buildNumber, boolean dryRun) throws IOException {
        HttpEntity entity = null;
        try (CloseableHttpResponse response = client.distributeBuild(buildName, buildNumber, distributionBuilder.dryRun(dryRun).build())) {
            entity = response.getEntity();
            return checkSuccess(response, dryRun, listener);
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    /**
     * Checks the status of promotion response and return true on success
     *
     * @param response
     * @param dryRun
     * @param listener
     * @return
     */
    private static boolean checkSuccess(HttpResponse response, boolean dryRun, TaskListener listener) {
        StatusLine status = response.getStatusLine();
        String content = "";
        try {
            content = ExtractorUtils.entityToString(response.getEntity());
            ExtractorUtils.validateStringNotBlank(content);
            JSONObject json = JSONObject.fromObject(content);
            String message = json.getString("message");
            if (assertResponseStatus(dryRun, listener, status, message)) {
                listener.getLogger().println(message);
                return true;
            }
        } catch (IOException e) {
            String parsingErrorStr = "Failed parsing distribution response";
            if (StringUtils.isNotBlank(content)) {
                parsingErrorStr = ": " + content;
            }
            e.printStackTrace(listener.error(parsingErrorStr));
        }
        return false;
    }

    private static boolean assertResponseStatus(boolean dryRun, TaskListener listener, StatusLine status, String message) {
        if (status.getStatusCode() != 200) {
            if (dryRun) {
                listener.error(
                        "Distribution failed during dry run (no change in Artifactory was done): " + status +
                                "\n" + message);
            } else {
                listener.error(
                        "Distribution failed. View Artifactory logs for more details: " + status + "\n" + message);
            }
            return false;
        }
        return true;
    }
}
