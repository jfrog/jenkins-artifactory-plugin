package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient.MD5_HEADER_NAME;
import static org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient.SHA1_HEADER_NAME;
import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

/**
 * @author yahavi
 **/
public class BuildAppendExecutor implements Executor {

    private final ArtifactoryServer pipelineServer;
    private final TaskListener listener;
    // The build info of the current build
    private final BuildInfo buildInfo;
    // The build number of the build to append
    private final String buildNumber;
    // The build name of the build to append
    private final String buildName;
    private final Run<?, ?> build;

    public BuildAppendExecutor(ArtifactoryServer pipelineServer, BuildInfo buildInfo, String buildName,
                               String buildNumber, Run<?, ?> build, TaskListener listener) {
        this.pipelineServer = pipelineServer;
        this.buildNumber = buildNumber;
        this.buildName = buildName;
        this.buildInfo = buildInfo;
        this.listener = listener;
        this.build = build;
    }

    @Override
    public void execute() throws Exception {
        ModuleBuilder moduleBuilder = new ModuleBuilder().id(buildName + "/" + buildNumber).type(ModuleType.BUILD);

        // Prepare Artifactory server
        org.jfrog.hudson.ArtifactoryServer server = Utils.prepareArtifactoryServer(null, pipelineServer);
        CredentialsConfig credentialsConfig = server.getResolverCredentialsConfig();
        Credentials credentials = credentialsConfig.provideCredentials(build.getParent());

        // Calculate build timestamp
        long timestamp = getBuildTimestamp(server, credentials);

        // Get checksum headers from the build info artifact
        addChecksumHeaders(server, credentials, moduleBuilder, timestamp);

        // Add module to build info
        buildInfo.getModules().add(moduleBuilder.build());
    }

    /**
     * Get build timestamp of the build to append. The build timestamp has to be converted to seconds from epoch.
     * For example, start time of: 2020-11-27T14:33:38.538+0200 should be converted to 1606480418538.
     *
     * @param server      - The Artifactory server
     * @param credentials - The credentials of the Artifactory serer
     * @return the build timestamp of the build to append.
     * @throws IOException    in case of any communication error with Artifactory, wrong credentials or if the build if missing.
     * @throws ParseException in case of the returned timestamp contains unexpected format.
     */
    private long getBuildTimestamp(org.jfrog.hudson.ArtifactoryServer server, Credentials credentials) throws IOException, ParseException {
        try (ArtifactoryBuildInfoClient client = server.createBuildInfoClientBuilder(credentials, createProxyConfiguration(), new JenkinsBuildInfoLog(listener)).build()) {
            Build build = client.getBuildInfo(buildName, buildNumber);
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            Date date = format.parse(build.getStarted());
            return date.getTime();
        }
    }

    /**
     * Download MD5 and SHA1 from the build info artifact. Add them to the current build info object.
     *
     * @param server        - The Artifactory server
     * @param credentials   - Credential of the Artifactory server
     * @param moduleBuilder - The module builder of the new module which will be added to the current build info object
     * @param timestamp     - The build timestamp of the build to append
     * @throws IOException if a header the build info object are missing.
     */
    private void addChecksumHeaders(org.jfrog.hudson.ArtifactoryServer server, Credentials credentials, ModuleBuilder moduleBuilder, long timestamp) throws IOException {
        // To support build name and numbers with ':', encode ':' by '%3A'. Further encoding will take place in 'getArtifactMetadata'.
        String encodedBuildName = buildName.replaceAll(":", "%3A");
        String encodedBuildNumber = buildNumber.replaceAll(":", "%3A");

        // Send HEAD request to <artifactory-url>/artifactory-build-info/<build-name>/<build-number>-timestamp.json to get the checksums
        String buildInfoPath = server.getArtifactoryUrl() + "/artifactory-build-info/" + encodedBuildName + "/" + encodedBuildNumber + "-" + timestamp + ".json";
        try (ArtifactoryDependenciesClient client = server.createArtifactoryDependenciesClient(credentials, createProxyConfiguration(), listener)) {
            // getArtifactMetadata return null response entity - there's no need to consume it
            HttpResponse response = client.getArtifactMetadata(buildInfoPath);
            moduleBuilder
                    .sha1(readHeader(response, buildInfoPath, SHA1_HEADER_NAME))
                    .md5(readHeader(response, buildInfoPath, MD5_HEADER_NAME));
        }
    }

    /**
     * Read a header from the HTTP response.
     *
     * @param response   - The HTTP response
     * @param path       - The path to the resource in Artifactory
     * @param headerName - The header name to search for
     * @return the header value.
     * @throws IOException if the requested header is missing.
     */
    private String readHeader(HttpResponse response, String path, String headerName) throws IOException {
        Header sha1 = response.getFirstHeader(headerName);
        if (sha1 == null) {
            throw new IOException("'" + headerName + "' header is missing in HEAD request to '" + path + "'");
        }
        return sha1.getValue();
    }
}
