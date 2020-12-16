package org.jfrog.hudson.jfpipelines;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.client.PreemptiveHttpClientBuilder;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.client.Version;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;
import org.jfrog.hudson.jfpipelines.payloads.JobStatusPayload;
import org.jfrog.hudson.jfpipelines.payloads.VersionPayload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.jfrog.hudson.util.SerializationUtils.createMapper;

public class JFrogPipelinesHttpClient implements AutoCloseable {
    static final Version MINIMAL_PIPELINES_VERSION = new Version("1.6.0");
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 300;
    private static final int DEFAULT_CONNECTION_RETRIES = 3;

    private ProxyConfiguration proxyConfiguration;
    private final String pipelinesIntegrationUrl;
    private PreemptiveHttpClient httpClient;
    private final String accessToken;
    private int connectionTimeout;
    private int connectionRetries;
    private final Log log;

    /**
     * JFrog Pipelines integration HTTP client.
     *
     * @param pipelinesIntegrationUrl - The JFrog Pipelines integration URL
     * @param accessToken             - The access toKen
     * @param log                     - The build log
     */
    public JFrogPipelinesHttpClient(String pipelinesIntegrationUrl, String accessToken, Log log) {
        this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
        this.connectionRetries = DEFAULT_CONNECTION_RETRIES;
        this.pipelinesIntegrationUrl = StringUtils.stripEnd(pipelinesIntegrationUrl, "/");
        this.accessToken = accessToken;
        this.log = log;
    }

    public JFrogPipelinesHttpClient(String pipelinesIntegrationUrl, String accessToken) {
        this(pipelinesIntegrationUrl, accessToken, new NullLog());
    }

    public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setConnectionRetries(int connectionRetries) {
        this.connectionRetries = connectionRetries;
    }

    @SuppressWarnings("unused")
    public int getConnectionRetries() {
        return this.connectionRetries;
    }

    @SuppressWarnings("unused")
    public ProxyConfiguration getProxyConfiguration() {
        return this.proxyConfiguration;
    }

    public PreemptiveHttpClient getHttpClient() {
        return getHttpClient(this.connectionTimeout);
    }

    public PreemptiveHttpClient getHttpClient(int connectionTimeout) {
        if (httpClient != null) {
            return httpClient;
        }
        PreemptiveHttpClientBuilder clientBuilder = new PreemptiveHttpClientBuilder().setConnectionRetries(this.connectionRetries).setTimeout(connectionTimeout).setLog(this.log);
        if (this.proxyConfiguration != null) {
            clientBuilder.setProxyConfiguration(this.proxyConfiguration);
        }
        clientBuilder.setAccessToken(this.accessToken);

        httpClient = clientBuilder.build();
        return httpClient;
    }

    /**
     * Get JFrog Pipelines version.
     *
     * @return Pipelines version
     * @throws IOException if response status is not 200 or 404.
     */
    public Version getVersion() throws IOException {
        String text = createMapper().writeValueAsString(new VersionPayload());
        log.debug("Sending version request to JFrog Pipelines with payload: " + text);
        HttpEntity requestEntity = new StringEntity(text, ContentType.APPLICATION_JSON);
        try (CloseableHttpResponse response = executePostRequest(requestEntity)) {
            HttpEntity httpEntity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_NOT_FOUND || httpEntity == null) {
                EntityUtils.consumeQuietly(httpEntity);
                return Version.NOT_FOUND;
            }
            if (statusCode != HttpStatus.SC_OK) {
                EntityUtils.consumeQuietly(httpEntity);
                throw new IOException(getMessageFromEntity(response.getEntity()));
            }
            try (InputStream content = httpEntity.getContent()) {
                JsonNode result = createMapper().readTree(content);
                log.debug("Version result: " + result);
                String version = result.get("version").asText();
                return new Version(version);
            }
        }
    }

    /**
     * Get and verify JFrog Pipelines version.
     *
     * @return Pipelines version
     * @throws VersionException if an error occurred or the Pipelines version is below minimum.
     */
    public Version verifyCompatibleVersion() throws VersionException {
        Version version;
        try {
            version = getVersion();
        } catch (IOException e) {
            throw new VersionException("Error occurred while requesting version information: " + e.getMessage(), e,
                    VersionCompatibilityType.NOT_FOUND);
        }
        if (version.isNotFound()) {
            throw new VersionException(
                    "There is either an incompatible version or no instance of JFrog Pipelines accessible at the provided URL.",
                    VersionCompatibilityType.NOT_FOUND);
        }
        if (!version.isAtLeast(MINIMAL_PIPELINES_VERSION)) {
            throw new VersionException("This plugin is compatible with version " + MINIMAL_PIPELINES_VERSION +
                    " or above of JFrog Pipelines. Please upgrade your JFrog Pipelines server!",
                    VersionCompatibilityType.INCOMPATIBLE);
        }
        return version;
    }

    public CloseableHttpResponse sendStatus(JobStatusPayload payload) throws IOException {
        String text = createMapper().writeValueAsString(payload);
        log.debug("Sending status to JFrog Pipelines with payload: " + text);
        StringEntity body = new StringEntity(text, ContentType.APPLICATION_JSON);
        return executePostRequest(body);
    }

    private CloseableHttpResponse executePostRequest(HttpEntity body) throws IOException {
        HttpPost httpPost = new HttpPost(pipelinesIntegrationUrl);
        httpPost.setEntity(body);
        return getHttpClient().execute(httpPost);
    }

    public String getMessageFromEntity(HttpEntity entity) throws IOException {
        if (entity == null) {
            return "";
        }
        String responseMessage = getResponseEntityContent(entity);
        if (StringUtils.isNotBlank(responseMessage)) {
            responseMessage = " Response message: " + responseMessage;
        }
        return responseMessage;
    }

    private String getResponseEntityContent(HttpEntity responseEntity) throws IOException {
        InputStream in = responseEntity.getContent();
        return StringUtils.defaultString(IOUtils.toString(in, StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
