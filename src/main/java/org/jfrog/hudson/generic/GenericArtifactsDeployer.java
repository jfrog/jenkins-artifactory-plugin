package org.jfrog.hudson.generic;

import com.google.common.collect.*;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.ModuleParallelDeployHelper;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.UploadSpecHelper;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.*;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.hudson.util.ProxyUtils.createProxyConfiguration;

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
        ArrayListMultimap<String, String> propertiesToAdd = getBuildPropertiesMap();
        ArtifactoryServer artifactoryServer = configurator.getArtifactoryServer();

        if (configurator.isUseSpecs()) {
            String spec = SpecUtils.getSpecStringFromSpecConf(configurator.getUploadSpec(), env, workingDir, listener.getLogger());
            artifactsToDeploy = workingDir.act(new FilesDeployerCallable(listener, spec, artifactoryServer,
                    credentialsConfig.provideCredentials(build.getParent()), propertiesToAdd,
                    createProxyConfiguration(), artifactoryServer.getDeploymentThreads()));
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
                    credentialsConfig.provideCredentials(build.getParent()), repositoryKey, propertiesToAdd,
                    createProxyConfiguration()));
        }
    }

    private ArrayListMultimap<String, String> getBuildPropertiesMap() {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
        String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(configurator, build);
        properties.put(BuildInfoFields.BUILD_NAME, buildName);
        properties.put(BuildInfoFields.BUILD_NUMBER, BuildUniqueIdentifierHelper.getBuildNumber(build));
        properties.put(BuildInfoFields.BUILD_TIMESTAMP, build.getTimestamp().getTime().getTime() + "");
        Utils.addParentBuildProps(properties, build);
        Utils.addVcsDetailsToProps(env, properties);
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
        private Map<String, Set<DeployDetails>> deployableArtifactsByModule;
        private int threads;

        // Generic deploy by pattern pairs
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

        // Generic deploy by spec
        public FilesDeployerCallable(TaskListener listener, String spec,
                                     ArtifactoryServer server, Credentials credentials,
                                     ArrayListMultimap<String, String> buildProperties, ProxyConfiguration proxyConfiguration, int threads) {
            this.listener = listener;
            this.spec = spec;
            this.server = server;
            this.credentials = credentials;
            this.buildProperties = buildProperties;
            this.proxyConfiguration = proxyConfiguration;
            this.threads = threads;
        }

        // Late deploy for build tools' deployable artifacts
        public FilesDeployerCallable(TaskListener listener, Map<String, Set<DeployDetails>> deployableArtifactsByModule,
                                     ArtifactoryServer server, Credentials credentials, ProxyConfiguration proxyConfiguration, int threads) {
            this.listener = listener;
            this.deployableArtifactsByModule = deployableArtifactsByModule;
            this.server = server;
            this.credentials = credentials;
            this.proxyConfiguration = proxyConfiguration;
            this.threads = threads;
        }

        public List<Artifact> invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
            Log log = new JenkinsBuildInfoLog(listener);

            // Create ArtifactoryClientBuilder
            ArtifactoryBuildInfoClientBuilder clientBuilder = server.createBuildInfoClientBuilder(credentials, proxyConfiguration, log);

            // Option 1. Upload - Use file specs.
            if (StringUtils.isNotEmpty(spec)) {
                SpecsHelper specsHelper = new SpecsHelper(log);
                try {
                    return specsHelper.uploadArtifactsBySpec(spec, threads, workspace, buildProperties, clientBuilder);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException("Failed uploading artifacts by spec", e);
                }
            }

            // Option 2. Maven & Gradle Pipeline late deploy - Deployable Artifacts by Module are already set.
            if (deployableArtifactsByModule != null) {
                try (ArtifactoryBuildInfoClient client = clientBuilder.build()) {
                    new ModuleParallelDeployHelper().deployArtifacts(client, deployableArtifactsByModule, threads);
                }
                return convertDeployDetailsByModuleToArtifacts(deployableArtifactsByModule);
            }

            // Option 3. Generic deploy - Fetch the artifacts details from workspace by using 'patternPairs'.
            Set<DeployDetails> artifactsToDeploy = Sets.newHashSet();
            Multimap<String, File> targetPathToFilesMap = buildTargetPathToFiles(workspace);
            for (Map.Entry<String, File> entry : targetPathToFilesMap.entries()) {
                artifactsToDeploy.addAll(buildDeployDetailsFromFileEntry(entry));
            }
            try (ArtifactoryBuildInfoClient client = clientBuilder.build()) {
                deploy(client, artifactsToDeploy);
                return convertDeployDetailsToArtifacts(artifactsToDeploy);
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

        private List<Artifact> convertDeployDetailsByModuleToArtifacts(Map<String, Set<DeployDetails>> detailsByModule) {
            List<Artifact> result = Lists.newArrayList();
            detailsByModule.forEach((module, details) -> {
                result.addAll(convertDeployDetailsToArtifacts(details));
            });
            return result;
        }

        public void deploy(ArtifactoryBuildInfoClient client, Set<DeployDetails> artifactsToDeploy)
                throws IOException {
            for (DeployDetails deployDetail : artifactsToDeploy) {
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

        public enum PatternType {
            ANT, WILDCARD
        }
    }
}
