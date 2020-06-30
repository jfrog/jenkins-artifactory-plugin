package org.jfrog.hudson.util;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.*;

import java.io.IOException;
import java.util.List;

import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

/**
 * @author Shay Yaakov
 */
public abstract class RepositoriesUtils {

    public static List<String> getReleaseRepositoryKeysFirst(DeployerOverrider deployer, ArtifactoryServer server) throws IOException {
        if (server == null) {
            return Lists.newArrayList();
        }

        return server.getReleaseRepositoryKeysFirst(deployer, null);
    }

    public static List<String> getSnapshotRepositoryKeysFirst(DeployerOverrider deployer, ArtifactoryServer server) throws IOException {
        if (server == null) {
            return Lists.newArrayList();
        }

        return server.getSnapshotRepositoryKeysFirst(deployer, null);
    }

    public static List<VirtualRepository> getVirtualRepositoryKeys(ResolverOverrider resolverOverrider,
                                                                   DeployerOverrider deployerOverrider,
                                                                   ArtifactoryServer server) {
        if (server == null) {
            return Lists.newArrayList();
        }

        return server.getVirtualRepositoryKeys(resolverOverrider, null);
    }

    public static List<VirtualRepository> generateVirtualRepos(ArtifactoryBuildInfoClient client) throws IOException {
        List<VirtualRepository> virtualRepositories;

        List<String> keys = client.getVirtualRepositoryKeys();
        virtualRepositories = Lists.newArrayList(Lists.transform(keys, new Function<String, VirtualRepository>() {
            public VirtualRepository apply(String from) {
                return new VirtualRepository(from, from);
            }
        }));

        return virtualRepositories;
    }

    public static List<VirtualRepository> getVirtualRepositoryKeys(String url, CredentialsConfig credentialsConfig,
                                                                   ArtifactoryServer artifactoryServer, Item item)
            throws IOException {
        List<VirtualRepository> virtualRepositories;
        CredentialsConfig preferredResolver = CredentialManager.getPreferredResolver(credentialsConfig, artifactoryServer);
        Credentials resolverCredentials = preferredResolver.provideCredentials(item);

        ArtifactoryBuildInfoClient client;
        if (StringUtils.isNotBlank(resolverCredentials.getUsername()) || StringUtils.isNotBlank(resolverCredentials.getAccessToken())) {
            client = new ArtifactoryBuildInfoClient(url, resolverCredentials.getUsername(), resolverCredentials.getPassword(),
                    resolverCredentials.getAccessToken(), new NullLog());
        } else {
            client = new ArtifactoryBuildInfoClient(url, new NullLog());
        }
        try {
            client.setConnectionTimeout(artifactoryServer.getTimeout());
            setRetryParams(artifactoryServer, client);

            if (Jenkins.get().proxy != null && !artifactoryServer.isBypassProxy()) {
                client.setProxyConfiguration(createProxyConfiguration());
            }

            virtualRepositories = RepositoriesUtils.generateVirtualRepos(client);
            return virtualRepositories;
        } finally {
            client.close();
        }
    }

    public static List<String> getLocalRepositories(String url, CredentialsConfig credentialsConfig,
                                                    ArtifactoryServer artifactoryServer, Item item) throws IOException {
        List<String> localRepository;
        CredentialsConfig preferredDeployer = CredentialManager.getPreferredDeployer(credentialsConfig, artifactoryServer);
        Credentials deployerCredentials = preferredDeployer.provideCredentials(item);

        ArtifactoryBuildInfoClient client;
        if (StringUtils.isNotBlank(deployerCredentials.getUsername()) || StringUtils.isNotBlank(deployerCredentials.getAccessToken())) {
            client = new ArtifactoryBuildInfoClient(url, deployerCredentials.getUsername(), deployerCredentials.getPassword(),
                    deployerCredentials.getAccessToken(), new NullLog());
        } else {
            client = new ArtifactoryBuildInfoClient(url, new NullLog());
        }
        try {
            client.setConnectionTimeout(artifactoryServer.getTimeout());
            setRetryParams(artifactoryServer, client);
            if (Jenkins.get().proxy != null && !artifactoryServer.isBypassProxy()) {
                client.setProxyConfiguration(createProxyConfiguration());
            }

            localRepository = client.getLocalRepositoriesKeys();
            return localRepository;
        } finally {
            client.close();
        }
    }

    public static ArtifactoryServer getArtifactoryServer(String artifactoryIdentity, List<ArtifactoryServer> artifactoryServers) {
        if (artifactoryServers != null) {
            for (ArtifactoryServer server : artifactoryServers) {
                if (server.getArtifactoryUrl().equals(artifactoryIdentity) || server.getServerId().equals(artifactoryIdentity)) {
                    return server;
                }
            }
        }
        return null;
    }

    /**
     * Returns the list of {@link org.jfrog.hudson.ArtifactoryServer} configured.
     *
     * @return can be empty but never null.
     */
    public static List<ArtifactoryServer> getArtifactoryServers() {
        ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl)
                Hudson.getInstance().getDescriptor(ArtifactoryBuilder.class);
        return descriptor.getArtifactoryServers();
    }

    public static List<Repository> createRepositoriesList(List<String> repositoriesValueList) {
        List<Repository> repositories = Lists.newArrayList();
        for (String repositoryKey : repositoriesValueList) {
            Repository repository = new Repository(repositoryKey);
            repositories.add(repository);
        }
        return repositories;
    }

    public static List<VirtualRepository> collectVirtualRepositories(List<VirtualRepository> repositories, String repoKey) {
        if (repositories == null) {
            repositories = Lists.newArrayList();
        }
        if (StringUtils.isNotBlank(repoKey)) {
            for (VirtualRepository vr : repositories) {
                if (repoKey.equals(vr.getDisplayName())) {
                    return repositories;
                }
            }
            VirtualRepository vr = new VirtualRepository(repoKey, repoKey);
            repositories.add(vr);
        }
        return repositories;
    }

    public static List<Repository> collectRepositories(String repoKey) {
        List<Repository> repositories = Lists.newArrayList();
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

    private static void setRetryParams(ArtifactoryServer artifactoryServer, ArtifactoryBuildInfoClient client) {
        setRetryParams(artifactoryServer.getConnectionRetry(), client);
    }

    /**
     * Sets the params of the retry mechanism
     *
     * @param connectionRetry - The max number of retries configured
     * @param client          - The client to set the values
     */
    public static void setRetryParams(int connectionRetry, ArtifactoryBaseClient client) {
        client.setConnectionRetries(connectionRetry);
    }
}
