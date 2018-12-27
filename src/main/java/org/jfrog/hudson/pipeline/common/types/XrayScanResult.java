package org.jfrog.hudson.pipeline.common.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.client.artifactoryXrayResponse.Summary;
import org.jfrog.hudson.pipeline.common.Utils;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by romang on 12/22/16.
 */
public class XrayScanResult implements Serializable {
    private ArtifactoryXrayResponse scanResult;

    public XrayScanResult(ArtifactoryXrayResponse result) {
        if (result == null) {
            throw new IllegalStateException("Invalid Xray scan result");
        }
        this.scanResult = result;
    }

    @Whitelisted
    public String toString() {
        try {
            return Utils.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(scanResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeJsonMappingException("Failed while processing the JSON result: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    public String getScanMessage() {
        Summary summary = scanResult.getSummary();
        if (summary != null) {
            return summary.getMessage();
        }
        throw new IllegalStateException("Failed while processing the JSON result: 'summary' field is missing. \n" + toString());
    }

    public String getScanUrl() {
        Summary summary = scanResult.getSummary();
        if (summary != null) {
            return summary.getMoreDetailsUrl();
        }
        throw new IllegalStateException("Failed while processing the JSON result: 'more_details_url' field is missing. \n" + toString());
    }

    public boolean isFoundVulnerable() {
        Summary summary = scanResult.getSummary();
        if (summary != null) {
            return summary.isFailBuild();
        }
        throw new IllegalStateException("Failed while processing the JSON result: 'fail_build' field is missing. \n" + toString());
    }
}
