package org.jfrog.hudson.pipeline.common.docker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.docker.utils.DockerUtils;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.ExtractorUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static org.jfrog.hudson.util.ExtractorUtils.entityToString;
import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

/**
 * Created by romang on 8/9/16.
 */
public class DockerImage implements Serializable {
    // Maximum time in milliseconds, after which this image can be removed from the images cache.
    private static final long MAX_AGE_MILLI = 12 * 60 * 60 * 1000L;
    // Stores the time this image is created, so that it can be later removed.
    private final long createdTime = System.currentTimeMillis();
    private final String imageId;
    private final String imageTag;
    private final String targetRepo;
    // List of build-info IDs. These IDs link this docker image to the specific corresponding
    // build-info instances.
    private int buildInfoId;
    private String manifest;
    private String agentName = "";
    // List of properties added to the build-info generated for this docker image.
    private Properties buildInfoModuleProps = new Properties();
    // Properties to be attached to the docker layers deployed to Artifactory.
    private ArrayListMultimap<String, String> artifactsProps;
    private final ArtifactoryVersion VIRTUAL_REPOS_SUPPORTED_VERSION = new ArtifactoryVersion("4.8.1");
    private String imagePath;

    public DockerImage(String imageId, String imageTag, String targetRepo, int buildInfoId,
           ArrayListMultimap<String, String> artifactsProps) {
        this.imageId = imageId;
        this.imageTag = imageTag;
        this.targetRepo = targetRepo;
        this.buildInfoId = buildInfoId;
        this.artifactsProps = artifactsProps;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdTime > MAX_AGE_MILLI;
    }

    public int getBuildInfoId() {
        return buildInfoId;
    }

    /**
     * Indicates whether a manifest has been captured and attached for this image.
     * @return
     */
    public boolean hasManifest() {
        return StringUtils.isNotBlank(manifest);
    }

    public String getImageTag() {
        return imageTag;
    }

    public String getImageId() {
        return imageId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    /**
     * Generates the build-info module for this docker image.
     * Additionally. this method tags the deployed docker layers with properties,
     * such as build.name, build.number and custom properties defined in the Jenkins build.
     * @param build
     * @param listener
     * @param config
     * @param buildName
     * @param buildNumber
     * @param timestamp
     * @return
     * @throws IOException
     */
    public Module generateBuildInfoModule(Run build, TaskListener listener, ArtifactoryConfigurator config, String buildName, String buildNumber, String timestamp) throws IOException {
        if (artifactsProps == null) {
            artifactsProps = ArrayListMultimap.create();
        }
        artifactsProps.put("build.name", buildName);
        artifactsProps.put("build.number", buildNumber);
        artifactsProps.put("build.timestamp", timestamp);
        String artifactsPropsStr = ExtractorUtils.buildPropertiesString(artifactsProps);

        Properties buildInfoItemsProps = new Properties();
        buildInfoItemsProps.setProperty("build.name", buildName);
        buildInfoItemsProps.setProperty("build.number", buildNumber);
        buildInfoItemsProps.setProperty("build.timestamp", timestamp);

        ArtifactoryServer server = config.getArtifactoryServer();
        CredentialsConfig preferredResolver = server.getDeployerCredentialsConfig();
        ArtifactoryDependenciesClient dependenciesClient = null;
        ArtifactoryBuildInfoClient propertyChangeClient = null;

        try {
            dependenciesClient = server.createArtifactoryDependenciesClient(preferredResolver.provideCredentials(build.getParent()),
                    createProxyConfiguration(), listener);

            CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(config, server);
            propertyChangeClient = server.createArtifactoryClient(preferredDeployer.provideCredentials(build.getParent()),
                    createProxyConfiguration());

            Module buildInfoModule = new Module();
            buildInfoModule.setId(imageTag.substring(imageTag.indexOf("/") + 1));

            try {
                findAndSetManifestFromArtifactory(server, dependenciesClient, listener);
            } catch (IOException e) {
                // The manifest could be found in Artifactory.
                // Yet, we do not fail the build, but return an empty build-info instead.
                // The reason for not failing build is that there's a chance that the image was replaced
                // with another image, deployed to the same repo path.
                // This can happen if two parallel jobs build the same image. In that case, the build-info
                // for this build will be empty.
                listener.getLogger().println("The manifest could not be fetched from Artifactory.");
                return buildInfoModule;
            }

            listener.getLogger().println("Fetching details of published docker layers from Artifactory...");
            setDependenciesAndArtifacts(buildInfoModule, artifactsPropsStr, buildInfoItemsProps,
                    dependenciesClient, propertyChangeClient, server);
            setBuildInfoModuleProps(buildInfoModule);
            return buildInfoModule;
        } finally {
            if (dependenciesClient != null) {
                dependenciesClient.close();
            }
            if (propertyChangeClient != null) {
                propertyChangeClient.close();
            }
        }
    }

    /**
     * Find and validate manifest.json file in Artifactory for the current image.
     * Since provided imageTag differs between reverse-proxy and proxy-less configuration, try to build the correct manifest path.
     * @param server
     * @param dependenciesClient
     * @param listener
     * @return
     * @throws IOException
     */
    private void findAndSetManifestFromArtifactory(ArtifactoryServer server, ArtifactoryDependenciesClient dependenciesClient, TaskListener listener) throws IOException {
        // Try to get manifest, assuming reverse proxy
        String proxyImagePath = DockerUtils.getImagePath(imageTag);
        String proxyManifestPath = StringUtils.join(new String[]{server.getArtifactoryUrl(), targetRepo, proxyImagePath, "manifest.json"}, "/");
        try {
            listener.getLogger().println("Trying to fetch manifest from Artifactory, assuming reverse proxy configuration.");
            checkAndSetManifestAndImagePathCandidates(proxyManifestPath, proxyImagePath, dependenciesClient, listener);
            return;
        } catch (IOException e) {
            listener.getLogger().println("The manifest could not be fetched from Artifactory, assuming reverse proxy configuration - " + e.getMessage());
            // Ignore - Artifactory may have a proxy-less setup. Let's try that.
        }

        // Try to get manifest, assuming proxy-less
        String proxyLessImagePath = proxyImagePath.substring(proxyImagePath.indexOf("/") + 1);
        String proxyLessManifestPath = StringUtils.join(new String[]{server.getArtifactoryUrl(), targetRepo, proxyLessImagePath, "manifest.json"}, "/");
        listener.getLogger().println("Trying to fetch manifest from Artifactory, assuming proxy-less configuration.");
        checkAndSetManifestAndImagePathCandidates(proxyLessManifestPath, proxyLessImagePath, dependenciesClient, listener);
    }

    /**
     * Check if the provided manifestPath is correct.
     * Set the manifest and imagePath in case of the correct manifest.
     * @param manifestPath
     * @param candidateImagePath
     * @param dependenciesClient
     * @throws IOException
     */
    private void checkAndSetManifestAndImagePathCandidates(String manifestPath, String candidateImagePath, ArtifactoryDependenciesClient dependenciesClient, TaskListener listener) throws IOException {
        String candidateManifest = getManifestFromArtifactory(dependenciesClient, manifestPath);

        String imageDigest = DockerUtils.getConfigDigest(candidateManifest);
        if (imageDigest.equals(imageId)) {
            manifest = candidateManifest;
            imagePath = candidateImagePath;
        }
    }

    private String getManifestFromArtifactory(ArtifactoryDependenciesClient dependenciesClient, String manifestPath) throws IOException {
        HttpResponse res = null;
        try {
            res = dependenciesClient.downloadArtifact(manifestPath);
            return IOUtils.toString(res.getEntity().getContent());
        } finally {
            if (res != null) {
                EntityUtils.consume(res.getEntity());
            }
        }
    }

    private void setBuildInfoModuleProps(Module buildInfoModule) {
        buildInfoModuleProps.setProperty("docker.image.id", DockerUtils.getShaValue(imageId));
        buildInfoModuleProps.setProperty("docker.captured.image", imageTag);
        buildInfoModule.setProperties(buildInfoModuleProps);
    }

    public void addBuildInfoModuleProps(Properties props) {
        buildInfoModuleProps.putAll(props);
    }

    private DockerLayers createLayers(ArtifactoryDependenciesClient dependenciesClient, String aql) throws IOException {
        AqlSearchResult result = dependenciesClient.searchArtifactsByAql(aql);

        DockerLayers layers = new DockerLayers();
        for (AqlSearchResult.SearchEntry entry : result.getResults()) {
            // Filtering out results with the wrong path.
            if (!StringUtils.equals(entry.getPath(), imagePath)) {
                continue;
            }

            // Filtering out results with the wrong repository.
            if (!StringUtils.equals(entry.getRepo(), targetRepo)) {
                Set<String> virtual_repos = Sets.newHashSet(entry.getVirtualRepos());
                if (!virtual_repos.contains(targetRepo)) {
                    continue;
                }
            }

            DockerLayer layer = new DockerLayer(entry);
            layers.addLayer(layer);
        }

        if (layers.getLayers().size() == 0) {
            throw new IllegalStateException(String.format("No docker layers found in Artifactory using AQL: %s after filtering layers in repos other than %s and with path other than %s", aql, targetRepo, imagePath));
        }

        return layers;
    }

    private void setDependenciesAndArtifacts(Module buildInfoModule, String artifactsProps, Properties buildInfoItemsProps, ArtifactoryDependenciesClient dependenciesClient, ArtifactoryBuildInfoClient propertyChangeClient, ArtifactoryServer server) throws IOException {
        String aql = getAqlQuery(propertyChangeClient);
        DockerLayers layers = createLayers(dependenciesClient, aql);
        DockerLayer historyLayer = layers.getByDigest(imageId);
        if (historyLayer == null) {
            throw new IllegalStateException("Could not find the history docker layer: " + imageId + " for image: " + imageTag + " in Artifactory.");
        }

        HttpResponse res = dependenciesClient.downloadArtifact(server.getArtifactoryUrl() + "/" + historyLayer.getFullPath());
        int dependencyLayerNum = DockerUtils.getNumberOfDependentLayers(ExtractorUtils.entityToString(res.getEntity()));

        LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
        LinkedHashSet<Artifact> artifacts = new LinkedHashSet<>();
        // Filter out duplicate layers from manifest by using HashSet.
        // Docker manifest may hold 'empty layers', as a result, docker promote will fail to promote the same layer more than once.
        Iterator<String> it = DockerUtils.getLayersDigests(manifest).iterator();
        for (int i = 0; i < dependencyLayerNum; i++) {
            String digest = it.next();
            DockerLayer layer = layers.getByDigest(digest);
            HttpResponse httpResponse = propertyChangeClient.executeUpdateFileProperty(layer.getFullPath(), artifactsProps);
            validateResponse(httpResponse);
            Dependency dependency = new DependencyBuilder().id(layer.getFileName()).sha1(layer.getSha1()).properties(buildInfoItemsProps).build();
            dependencies.add(dependency);

            Artifact artifact = new ArtifactBuilder(layer.getFileName()).sha1(layer.getSha1()).properties(buildInfoItemsProps).build();
            artifacts.add(artifact);
        }
        buildInfoModule.setDependencies(new ArrayList<>(dependencies));

        while (it.hasNext()) {
            String digest = it.next();
            DockerLayer layer = layers.getByDigest(digest);
            if (layer == null) {
                continue;
            }
            HttpResponse httpResponse = propertyChangeClient.executeUpdateFileProperty(layer.getFullPath(), artifactsProps);
            validateResponse(httpResponse);
            Artifact artifact = new ArtifactBuilder(layer.getFileName()).sha1(layer.getSha1()).properties(buildInfoItemsProps).build();
            artifacts.add(artifact);
        }
        buildInfoModule.setArtifacts(new ArrayList<>(artifacts));
    }

    /**
     * Prepare AQL query to get all the manifest layers from Artifactory.
     * Needed for build-info sha1/md5 checksum for each artifact and dependency.
     *
     * @return
     * @throws IOException
     */
    private String getAqlQuery(ArtifactoryBuildInfoClient propertyChangeClient) throws IOException {
        boolean includeVirtualRepos = propertyChangeClient.getArtifactoryVersion().isAtLeast(VIRTUAL_REPOS_SUPPORTED_VERSION);
        List<String> layersDigest = DockerUtils.getLayersDigests(manifest);
        StringBuilder aqlRequestForDockerSha = new StringBuilder("items.find({")
            .append("\"path\":\"").append(imagePath).append("\",\"$or\":[");

        List<String> layersQuery = new ArrayList<String>();
        for (String digest : layersDigest) {
            String shaVersion = DockerUtils.getShaVersion(digest);

            String singleFileQuery;
            if (StringUtils.equalsIgnoreCase(shaVersion, "sha1")) { ;
                singleFileQuery = String.format("{\"actual_sha1\": \"%s\"}", DockerUtils.getShaValue(digest));
            } else {
                singleFileQuery = String.format("{\"name\": \"%s\"}", DockerUtils.digestToFileName(digest));
            }
            layersQuery.add(singleFileQuery);
        }

        aqlRequestForDockerSha.append(StringUtils.join(layersQuery, ","));
        if (includeVirtualRepos) {
            aqlRequestForDockerSha.append("]}).include(\"name\",\"repo\",\"path\",\"actual_sha1\",\"virtual_repos\")");
        } else {
            aqlRequestForDockerSha.append("]}).include(\"name\",\"repo\",\"path\",\"actual_sha1\")");
        }
        return aqlRequestForDockerSha.toString();
    }

    private void validateResponse(HttpResponse httpResponse) throws IOException {
        int code = httpResponse.getStatusLine().getStatusCode();
        if (code != 204) {
            String response = entityToString(httpResponse.getEntity());
            throw new IOException("Failed while trying to set properties on docker layer: " + response);
        }
    }
}
