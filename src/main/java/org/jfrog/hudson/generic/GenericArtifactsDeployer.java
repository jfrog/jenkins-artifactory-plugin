package org.jfrog.hudson.generic;

import com.google.common.collect.*;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.UploadSpecHelper;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.util.*;

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

    private Run build;
    private ArtifactoryGenericConfigurator configurator;
    private BuildListener listener;
    private CredentialsConfig credentialsConfig;
    private EnvVars env;
    private List<Artifact> artifactsToDeploy = Lists.newArrayList();

    public GenericArtifactsDeployer(Run build, ArtifactoryGenericConfigurator configurator,
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
        FilePath workingDir = build.getExecutor().getCurrentWorkspace();
        ArrayListMultimap<String, String> propertiesToAdd = getbuildPropertiesMap();
        ArtifactoryServer artifactoryServer = configurator.getArtifactoryServer();

        if (configurator.isUseSpecs()) {
            String spec = SpecUtils.getSpecStringFromSpecConf(configurator.getUploadSpec(), env, workingDir, listener.getLogger());
            artifactsToDeploy = workingDir.act(new FilesDeployerCallable(listener, spec, artifactoryServer,
                    credentialsConfig.getCredentials(build.getParent()), propertiesToAdd,
                    ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy)));
        } else {
            String deployPattern = Util.replaceMacro(configurator.getDeployPattern(), env);
            deployPattern = StringUtils.replace(deployPattern, "\r\n", "\n");
            deployPattern = StringUtils.replace(deployPattern, ",", "\n");
            Multimap<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(deployPattern);
            if (pairs.isEmpty()) {
                return;
            }
            String repositoryKey = Util.replaceMacro(configurator.getRepositoryKey(), env);
            artifactsToDeploy = workingDir.act(new FilesDeployerCallable(listener, pairs, artifactoryServer,
                    credentialsConfig.getCredentials(build.getParent()), repositoryKey, propertiesToAdd,
                    ArtifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy)));
        }
    }

    private ArrayListMultimap<String, String> getbuildPropertiesMap() {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
        String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(configurator, build);
        properties.put("build.name", buildName);
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
        properties.putAll(PropertyUtils.getDeploymentPropertiesMap(configurator.getDeploymentProperties(), env));

        return properties;
    }

    public static class FilesDeployerCallable extends MasterToSlaveFileCallable<List<Artifact>> {

        private String repositoryKey;
        private TaskListener listener;
        private Multimap<String, String> patternPairs;
        private ArtifactoryServer server;
        private Credentials credentials;
        private ArrayListMultimap<String, String> buildProperties;
        private ProxyConfiguration proxyConfiguration;
        private PatternType patternType = PatternType.ANT;
        private String spec;
        private Set<DeployDetails> deployableArtifacts;

        public enum PatternType {
            ANT, WILDCARD
        }

        public FilesDeployerCallable(TaskListener listener, Multimap<String, String> patternPairs,
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

        public FilesDeployerCallable(TaskListener listener, String spec,
                                     ArtifactoryServer server, Credentials credentials,
                                     ArrayListMultimap<String, String> buildProperties, ProxyConfiguration proxyConfiguration) {
            this.listener = listener;
            this.spec = spec;
            this.server = server;
            this.credentials = credentials;
            this.buildProperties = buildProperties;
            this.proxyConfiguration = proxyConfiguration;
        }

        public FilesDeployerCallable(TaskListener listener, Set<DeployDetails> deployableArtifacts,
                                     ArtifactoryServer server, Credentials credentials,
                                     ProxyConfiguration proxyConfiguration) {
            this.listener = listener;
            this.deployableArtifacts = deployableArtifacts;
            this.server = server;
            this.credentials = credentials;
            this.proxyConfiguration = proxyConfiguration;
        }

        public List<Artifact> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
            Set<DeployDetails> artifactsToDeploy;
            Log log = new JenkinsBuildInfoLog(listener);

            // Create ArtifactoryClientBuilder
            ArtifactoryBuildInfoClientBuilder clientBuilder = server.createArtifactoryClientBuilder(credentials.getUsername(),
                    credentials.getPassword(), proxyConfiguration, log);

            if (StringUtils.isNotEmpty(spec)) {
                // Option 1. Upload - Use file specs.
                SpecsHelper specsHelper = new SpecsHelper(log);
                try {
                    return specsHelper.uploadArtifactsBySpec(spec, server.getDeploymentThreads(), workspace, buildProperties, clientBuilder);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Failed uploading artifacts by spec", e);
                }
            } else {
                if (deployableArtifacts != null) {
                    // Option 2. Pipeline deploy - There is already a deployable artifacts set.
                    artifactsToDeploy = deployableArtifacts;
                } else {
                    // Option 3. Generic deploy - Fetch the artifacts details from workspace by using 'patternPairs'.
                    artifactsToDeploy = Sets.newHashSet();
                    Multimap<String, File> targetPathToFilesMap = buildTargetPathToFiles(workspace);
                    for (Map.Entry<String, File> entry : targetPathToFilesMap.entries()) {
                        artifactsToDeploy.addAll(buildDeployDetailsFromFileEntry(entry));
                    }
                }
                try (ArtifactoryBuildInfoClient client = clientBuilder.build()) {
                    deploy(client, artifactsToDeploy);
                    return convertDeployDetailsToArtifacts(artifactsToDeploy);
                }
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
                deploymentPathBuilder.append("/").append(deployDetail.getTargetRepository());
                if (!deployDetail.getArtifactPath().startsWith("/")) {
                    deploymentPathBuilder.append("/");
                }
                deploymentPathBuilder.append(deployDetail.getArtifactPath());
                client.deployArtifact(deployDetail);
            }
        }

        private Multimap<String, File> buildTargetPathToFiles(File workspace) throws IOException {
            Multimap<String, File> result = HashMultimap.create();
            if (patternPairs == null) {
                return result;
            }
            for (Map.Entry<String, String> entry : patternPairs.entries()) {
                String pattern = entry.getKey();
                String targetPath = entry.getValue();
                Multimap<String, File> publishingData =
                    PublishedItemsHelper.buildPublishingData(workspace, pattern, targetPath);

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
            String path;
            if (patternType == PatternType.ANT) {
                path = PublishedItemsHelper.calculateTargetPath(targetPath, artifactFile);
            } else {
                path = UploadSpecHelper.wildcardCalculateTargetPath(targetPath, artifactFile);
            }
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
