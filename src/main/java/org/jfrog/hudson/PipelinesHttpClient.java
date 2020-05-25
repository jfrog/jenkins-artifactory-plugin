package org.jfrog.hudson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.client.PreemptiveHttpClientBuilder;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.util.URI;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PipelinesHttpClient implements AutoCloseable {
    private static final ArtifactoryVersion MINIMAL_PIPELINES_VERSION = new ArtifactoryVersion("1.5.0");
    private static final String VERSION_INFO_URL = "/api/v1/system/info";
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 300;
    private static final int DEFAULT_CONNECTION_RETRY = 3;

    private ProxyConfiguration proxyConfiguration;
    private PreemptiveHttpClient httpClient;
    private final String pipelinesUrl;
    private final String accessToken;
    private int connectionTimeout;
    private int connectionRetries;
    private boolean insecureTls;
    private final Log log;

    public PipelinesHttpClient(String pipelinesUrl, String accessToken, Log log) {
        this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
        this.connectionRetries = DEFAULT_CONNECTION_RETRY;
        this.insecureTls = false;
        this.pipelinesUrl = StringUtils.stripEnd(pipelinesUrl, "/");
        this.accessToken = accessToken;
        this.log = log;
    }

    public static String encodeUrl(String unescaped) {
        byte[] rawData = URLCodec.encodeUrl(URI.allowed_query, org.apache.commons.codec.binary.StringUtils.getBytesUtf8(unescaped));
        return org.apache.commons.codec.binary.StringUtils.newStringUsAscii(rawData);
    }

    public void setProxyConfiguration(String host, int port) {
        this.setProxyConfiguration(host, port, null, null);
    }

    public void setProxyConfiguration(ProxyConfiguration proxy) {
        this.setProxyConfiguration(proxy.host, proxy.port, proxy.username, proxy.password);
    }

    public void setProxyConfiguration(String host, int port, String username, String password) {
        this.proxyConfiguration = new ProxyConfiguration();
        this.proxyConfiguration.host = host;
        this.proxyConfiguration.port = port;
        this.proxyConfiguration.username = username;
        this.proxyConfiguration.password = password;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setConnectionRetries(int connectionRetries) {
        this.connectionRetries = connectionRetries;
    }

    public void setInsecureTls(boolean insecureTls) {
        this.insecureTls = insecureTls;
    }

    public int getConnectionRetries() {
        return this.connectionRetries;
    }

    public ProxyConfiguration getProxyConfiguration() {
        return this.proxyConfiguration;
    }

    public PreemptiveHttpClient getHttpClient() {
        return this.getHttpClient(this.connectionTimeout);
    }

    public PreemptiveHttpClient getHttpClient(int connectionTimeout) {
        if (httpClient != null) {
            return httpClient;
        }
        PreemptiveHttpClientBuilder clientBuilder = (new PreemptiveHttpClientBuilder()).setConnectionRetries(this.connectionRetries).setInsecureTls(this.insecureTls).setTimeout(connectionTimeout).setLog(this.log);
        if (this.proxyConfiguration != null) {
            clientBuilder.setProxyConfiguration(this.proxyConfiguration);
        }
        clientBuilder.setAccessToken(this.accessToken);

        httpClient = clientBuilder.build();
        return httpClient;
    }

    /**
     * Get Pipelines version.
     *
     * @return Pipelines version
     * @throws IOException if response status is not 200 or 404.
     */
    public ArtifactoryVersion getVersion() throws IOException {
        String versionUrl = pipelinesUrl + VERSION_INFO_URL;
        HttpResponse response = executeGetRequest(versionUrl);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_NOT_FOUND) {
                return ArtifactoryVersion.NOT_FOUND;
            }
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException(getMessageFromEntity(response.getEntity()));
            }
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity == null) {
                return ArtifactoryVersion.NOT_FOUND;
            }
            try (InputStream content = httpEntity.getContent()) {
                JsonNode result = getJsonNode(content);
                log.debug("Version result: " + result);
                String version = result.get("version").asText();
                return new ArtifactoryVersion(version);
            }
        } finally {
            consumeEntity(response);
        }
    }

    /**
     * Get and verify Pipelines version.
     *
     * @return Pipelines version
     * @throws VersionException if an error occurred or the Pipelines version is below minimum.
     */
    public ArtifactoryVersion verifyCompatiblePipelinesVersion() throws VersionException {
        ArtifactoryVersion version;
        try {
            version = getVersion();
        } catch (IOException e) {
            throw new VersionException("Error occurred while requesting version information: " + e.getMessage(), e,
                    VersionCompatibilityType.NOT_FOUND);
        }
        if (version.isNotFound()) {
            throw new VersionException(
                    "There is either an incompatible or no instance of Pipelines at the provided URL.",
                    VersionCompatibilityType.NOT_FOUND);
        }
        if (!version.isAtLeast(MINIMAL_PIPELINES_VERSION)) {
            throw new VersionException("This plugin is compatible with version " + MINIMAL_PIPELINES_VERSION +
                    " of JFrog Pipelines and above. Please upgrade your JFrog Pipelines server!",
                    VersionCompatibilityType.INCOMPATIBLE);
        }
        return version;
    }

    public JsonNode getJsonNode(InputStream content) throws IOException {
        JsonFactory jsonFactory = this.createJsonFactory();
        JsonParser parser = jsonFactory.createParser(content);
        return (JsonNode) parser.readValueAsTree();
    }

    private HttpResponse executeGetRequest(String lastModifiedUrl) throws IOException {
        PreemptiveHttpClient client = this.getHttpClient();
        HttpGet httpGet = new HttpGet(lastModifiedUrl);
        return client.execute(httpGet);
    }

    private void consumeEntity(HttpResponse response) throws IOException {
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
            EntityUtils.consume(httpEntity);
        }
    }

    public JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
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
