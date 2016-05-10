package org.jfrog.hudson.generic;

import com.google.common.collect.*;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.ExtractorUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deploys artifacts to Artifactory. This class is used only in free style generic configurator.
 *
 * @author Shay Yaakov
 */
public class GenericArtifactsDeployer {
    private static final String SHA1 = "SHA1";
    private static final String MD5 = "MD5";

    private AbstractBuild build;
    private ArtifactoryGenericConfigurator configurator;
    private BuildListener listener;
    private CredentialsConfig credentialsConfig;
    private EnvVars env;
    private List<Artifact> artifactsToDeploy = Lists.newArrayList();

    public GenericArtifactsDeployer(AbstractBuild build, ArtifactoryGenericConfigurator configurator,
                                    BuildListener listener, CredentialsConfig credentialsConfig)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        this.build = build;
        this.configurator = configurator;
        this.listener = listener;
        this.credentialsConfig = credentialsConfig;
        this.env = build.getEnvironment(listener);
    }

    public List<Artifact> getDeployedArtifacts() {
        return artifactsToDeploy;
    }

    public void deploy()
            throws IOException, InterruptedException {
        String deployPattern = Util.replaceMacro(configurator.getDeployPattern(), env);
        deployPattern = StringUtils.replace(deployPattern, "\r\n", "\n");
        deployPattern = StringUtils.replace(deployPattern, ",", "\n");
        Multimap<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(deployPattern);
        if (pairs.isEmpty()) {
            return;
        }

        FilePath workingDir = build.getWorkspace();
        ArrayListMultimap<String, String> propertiesToAdd = getbuildPropertiesMap();
        ArtifactoryServer artifactoryServer = configurator.getArtifactoryServer();
        String repositoryKey = Util.replaceMacro(configurator.getRepositoryKey(), env);
        artifactsToDeploy = workingDir.act(new FilesDeployerCallable(listener, pairs, artifactoryServer,
                credentialsConfig.getCredentials(), repositoryKey, propertiesToAdd,
                artifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy)));
    }

    private ArrayListMultimap<String, String> getbuildPropertiesMap() {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();

        properties.put("build.name", BuildUniqueIdentifierHelper.getBuildName(build, listener));
        properties.put("build.number", BuildUniqueIdentifierHelper.getBuildNumber(build));
        properties.put("build.timestamp", build.getTimestamp().getTime().getTime() + "");
        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(build);
        if (parent != null) {
            properties.put("build.parentName", ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject()));
            properties.put("build.parentNumber", parent.getUpstreamBuild() + "");
        }
        String revision = ExtractorUtils.getVcsRevision(env);
        if (StringUtils.isNotBlank(revision)) {
            properties.put(BuildInfoFields.VCS_REVISION, revision);
        }

        addMatrixParams(properties);

        return properties;
    }

    private void addMatrixParams(Multimap<String, String> properties) {
        String[] matrixParams = StringUtils.split(configurator.getMatrixParams(), ";");
        if (matrixParams == null) {
            return;
        }
        for (String matrixParam : matrixParams) {
            String[] split = StringUtils.split(matrixParam, '=');
            if (split.length == 2) {
                String value = Util.replaceMacro(split[1], env);
                //Space is not allowed in property key
                properties.put(split[0].replace(" ", StringUtils.EMPTY), value);
            }
        }
    }

    private static class FilesDeployerCallable implements FilePath.FileCallable<List<Artifact>> {

        private final String repositoryKey;
        private BuildListener listener;
        private Multimap<String, String> patternPairs;
        private ArtifactoryServer server;
        private Credentials credentials;
        private ArrayListMultimap<String, String> buildProperties;
        private ProxyConfiguration proxyConfiguration;

        public FilesDeployerCallable(BuildListener listener, Multimap<String, String> patternPairs,
                                     ArtifactoryServer server, Credentials credentials, String repositoryKey,
                                     ArrayListMultimap<String, String> buildProperties, ProxyConfiguration proxyConfiguration) {
            this.listener = listener;
            this.patternPairs = patternPairs;
            this.server = server;
            this.credentials = credentials;
            this.repositoryKey = repositoryKey;
            this.buildProperties = buildProperties;
            this.proxyConfiguration = proxyConfiguration;
        }

        public List<Artifact> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
            Multimap<String, File> targetPathToFilesMap = buildTargetPathToFiles(workspace);
            Set<DeployDetails> artifactsToDeploy = Sets.newHashSet();
            for (Map.Entry<String, File> entry : targetPathToFilesMap.entries()) {
                artifactsToDeploy.addAll(buildDeployDetailsFromFileEntry(entry));
            }

            ArtifactoryBuildInfoClient client = server.createArtifactoryClient(credentials.getUsername(),
                    credentials.getPassword(), proxyConfiguration);
            try {
                deploy(client, artifactsToDeploy);
                return convertDeployDetailsToArtifacts(artifactsToDeploy);
            } finally {
                client.shutdown();
            }
        }

        private List<Artifact> convertDeployDetailsToArtifacts(Set<DeployDetails> details) {
            List<Artifact> result = Lists.newArrayList();
            for (DeployDetails detail : details) {
                String ext = FilenameUtils.getExtension(detail.getFile().getName());
                Artifact artifact = new ArtifactBuilder(detail.getFile().getName()).md5(detail.getMd5())
                        .sha1(detail.getSha1()).type(ext).build();
                result.add(artifact);
            }
            return result;
        }

        public void deploy(ArtifactoryBuildInfoClient client, Set<DeployDetails> artifactsToDeploy)
                throws IOException {
            for (DeployDetails deployDetail : artifactsToDeploy) {
                StringBuilder deploymentPathBuilder = new StringBuilder(server.getUrl());
                deploymentPathBuilder.append("/").append(repositoryKey);
                if (!deployDetail.getArtifactPath().startsWith("/")) {
                    deploymentPathBuilder.append("/");
                }
                deploymentPathBuilder.append(deployDetail.getArtifactPath());
                listener.getLogger().println("Deploying artifact: " + deploymentPathBuilder.toString());
                client.deployArtifact(deployDetail);
            }
        }

        private Multimap<String, File> buildTargetPathToFiles(File workspace) throws IOException {
            Multimap<String, File> result = HashMultimap.create();
            for (Map.Entry<String, String> entry : patternPairs.entries()) {
                String pattern = entry.getKey();
                String targetPath = entry.getValue();
                Multimap<String, File> publishingData = PublishedItemsHelper.buildPublishingData(workspace, pattern,
                        targetPath);
                if (publishingData != null) {
                    listener.getLogger().println(
                            "For pattern: " + pattern + " " + publishingData.size() + " artifacts were found");
                    result.putAll(publishingData);
                } else {
                    listener.getLogger().println("For pattern: " + pattern + " no artifacts were found");
                }
            }

            return result;
        }

        private Set<DeployDetails> buildDeployDetailsFromFileEntry(Map.Entry<String, File> fileEntry)
                throws IOException {
            Set<DeployDetails> result = Sets.newHashSet();
            String targetPath = fileEntry.getKey();
            File artifactFile = fileEntry.getValue();
            String path = PublishedItemsHelper.calculateTargetPath(targetPath, artifactFile);
            path = StringUtils.replace(path, "//", "/");

            // calculate the sha1 checksum that is not given by Jenkins and add it to the deploy artifactsToDeploy
            Map<String, String> checksums = Maps.newHashMap();
            try {
                checksums = FileChecksumCalculator.calculateChecksums(artifactFile, SHA1, MD5);
            } catch (NoSuchAlgorithmException e) {
                listener.getLogger().println("Could not find checksum algorithm for " + SHA1 + " or " + MD5);
            }
            DeployDetails.Builder builder = new DeployDetails.Builder()
                    .file(artifactFile)
                    .artifactPath(path)
                    .targetRepository(repositoryKey)
                    .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                    .addProperties(buildProperties);
            result.add(builder.build());

            return result;
        }
    }
}
