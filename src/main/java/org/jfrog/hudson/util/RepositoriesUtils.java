package org.jfrog.hudson.util;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.hudson.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

/**
 * @author Shay Yaakov
 */
public abstract class RepositoriesUtils {

    public static List<String> getReleaseRepositoryKeysFirst(DeployerOverrider deployer, ArtifactoryServer server) throws IOException {
        if (server == null) {
            return new ArrayList<>();
        }

        return server.getReleaseRepositoryKeysFirst(deployer, null);
    }

    public static List<String> getSnapshotRepositoryKeysFirst(DeployerOverrider deployer, ArtifactoryServer server) throws IOException {
        if (server == null) {
            return new ArrayList<>();
        }

        return server.getSnapshotRepositoryKeysFirst(deployer, null);
    }

    public static List<ResolutionRepository> getResolutionRepositoryKeys(ResolverOverrider resolverOverrider,
                                                                         DeployerOverrider deployerOverrider,
                                                                         ArtifactoryServer server) {
        if (server == null) {
            return new ArrayList<>();
        }

        return server.getVirtualRepositoryKeys(resolverOverrider, null);
    }

    public static List<ResolutionRepository> generateResolutionRepos(ArtifactoryManager artifactoryManager) throws IOException {
        List<String> resolutionRepoKeys = artifactoryManager.getRemoteRepositoriesKeys();
        if (resolutionRepoKeys == null) {
            resolutionRepoKeys = new ArrayList<>();
        }

        List<String> virtualRepoKeys = artifactoryManager.getVirtualRepositoriesKeys();
        if (virtualRepoKeys != null) {
            resolutionRepoKeys.addAll(virtualRepoKeys);
        }

        return resolutionRepoKeys.stream().map(repoKey -> new ResolutionRepository(repoKey, repoKey)).collect(Collectors.toList());
    }

    public static List<Repository> generateDeploymentRepos(ArtifactoryManager artifactoryManager) throws IOException {
        List<String> deploymentRepoKeys = artifactoryManager.getLocalRepositoriesKeys();
        if (deploymentRepoKeys == null) {
            deploymentRepoKeys = new ArrayList<>();
        }

        List<String> virtualRepoKeys = artifactoryManager.getVirtualRepositoriesKeys();
        if (virtualRepoKeys != null) {
            deploymentRepoKeys.addAll(virtualRepoKeys);
        }
        List<String> federatedRepoKeys = artifactoryManager.getFederatedRepositoriesKeys();
        if (federatedRepoKeys != null) {
            deploymentRepoKeys.addAll(federatedRepoKeys);
        }

        return deploymentRepoKeys.stream().map(Repository::new).collect(Collectors.toList());
    }

    public static List<ResolutionRepository> getResolutionRepositoryKeys(String url, CredentialsConfig credentialsConfig,
                                                                         ArtifactoryServer artifactoryServer, Item item)
            throws IOException {
        CredentialsConfig preferredResolver = CredentialManager.getPreferredResolver(credentialsConfig, artifactoryServer);
        Credentials resolverCredentials = preferredResolver.provideCredentials(item);

        try (ArtifactoryManager artifactoryManager = createArtifactoryManager(url, resolverCredentials, artifactoryServer)) {
            return RepositoriesUtils.generateResolutionRepos(artifactoryManager);
        }
    }

    public static List<Repository> getDeploymentRepositories(String url, CredentialsConfig credentialsConfig,
                                                             ArtifactoryServer artifactoryServer, Item item) throws IOException {
        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(credentialsConfig, artifactoryServer);
        Credentials deployerCredentials = preferredDeployer.provideCredentials(item);

        try (ArtifactoryManager artifactoryManager = createArtifactoryManager(url, deployerCredentials, artifactoryServer)) {
            return generateDeploymentRepos(artifactoryManager);
        }
    }

    private static ArtifactoryManager createArtifactoryManager(String url, Credentials credentials, ArtifactoryServer artifactoryServer) {
        ArtifactoryManager artifactoryManager;
        if (StringUtils.isNotBlank(credentials.getUsername()) || StringUtils.isNotBlank(credentials.getAccessToken())) {
            artifactoryManager = new ArtifactoryManager(url, credentials.getUsername(), credentials.getPassword(),
                    credentials.getAccessToken(), new NullLog());
        } else {
            artifactoryManager = new ArtifactoryManager(url, new NullLog());
        }
        artifactoryManager.setConnectionTimeout(artifactoryServer.getTimeout());
        setRetryParams(artifactoryServer, artifactoryManager);
        if (Jenkins.get().proxy != null && !artifactoryServer.isBypassProxy()) {
            artifactoryManager.setProxyConfiguration(createProxyConfiguration());
        }
        return artifactoryManager;
    }

    /**
     * Search for Artifactory server by `key` (could be Artifactory server URL or serverId).
     *
     * @param key - The key on which to do a search.
     * @return - ArtifactoryServer
     */
    public static ArtifactoryServer getArtifactoryServer(String key) {
        JFrogPlatformInstance JFrogPlatformInstance = getJFrogPlatformInstances(key);
        if (JFrogPlatformInstance != null) {
            return JFrogPlatformInstance.getArtifactory();
        }
        return null;
    }

    /**
     * Search for JFrog instance by `key` (could be Artifactory server URL or serverId).
     *
     * @param key - The key on which to do a search.
     * @return - JFrogPlatformInstance
     */
    public static JFrogPlatformInstance getJFrogPlatformInstances(String key) {
        List<JFrogPlatformInstance> jfrogInstances = getJFrogPlatformInstances();
        if (jfrogInstances != null && jfrogInstances.size() > 0) {
            for (JFrogPlatformInstance JFrogPlatformInstance : jfrogInstances) {
                if (JFrogPlatformInstance.getArtifactory().getArtifactoryUrl().equals(key) || JFrogPlatformInstance.getArtifactory().getServerId().equals(key)) {
                    return JFrogPlatformInstance;
                }
            }
        }
        return null;
    }

    /**
     * Returns the list of {@link JFrogPlatformInstance} configured.
     *
     * @return can be empty but never null.
     */
    public static List<JFrogPlatformInstance> getJFrogPlatformInstances() {
        ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl)
                Hudson.get().getDescriptor(ArtifactoryBuilder.class);
        return descriptor.getJfrogInstances();
    }

    public static List<Repository> createRepositoriesList(List<String> repositoriesValueList) {
        List<Repository> repositories = new ArrayList<>();
        for (String repositoryKey : repositoriesValueList) {
            Repository repository = new Repository(repositoryKey);
            repositories.add(repository);
        }
        return repositories;
    }

    public static List<ResolutionRepository> collectResolutionRepositories(List<ResolutionRepository> repositories, String repoKey) {
        if (repositories == null) {
            repositories = new ArrayList<>();
        }
        if (StringUtils.isNotBlank(repoKey)) {
            for (ResolutionRepository repository : repositories) {
                if (repoKey.equals(repository.getDisplayName())) {
                    return repositories;
                }
            }
            repositories.add(new ResolutionRepository(repoKey, repoKey));
        }
        return repositories;
    }

    public static List<Repository> collectRepositories(String repoKey) {
        List<Repository> repositories = new ArrayList<>();
        if (StringUtils.isNotBlank(repoKey)) {
            Repository r = new Repository(repoKey);
            repositories.add(r);
        }
        return repositories;
    }

    public static void validateServerConfig(AbstractBuild build, BuildListener listener,
                                            ArtifactoryServer artifactoryServer, String artifactoryUrl) throws IOException {
        if (artifactoryServer == null) {
            String error = "No Artifactory server configured for " + artifactoryUrl +
                    ". Please check your configuration.";
            listener.getLogger().println(error);
            throw new IOException(error);
        }
    }

    private static void setRetryParams(ArtifactoryServer artifactoryServer, ArtifactoryManager artifactoryManager) {
        setRetryParams(artifactoryServer.getConnectionRetry(), artifactoryManager);
    }

    /**
     * Sets the params of the retry mechanism
     *
     * @param connectionRetry    - The max number of retries configured
     * @param artifactoryManager - The ArtifactoryManager to set the values
     */
    public static void setRetryParams(int connectionRetry, ArtifactoryManager artifactoryManager) {
        artifactoryManager.setConnectionRetries(connectionRetry);
    }
}
