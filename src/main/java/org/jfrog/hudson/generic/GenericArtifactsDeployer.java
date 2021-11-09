package org.jfrog.hudson.generic;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
import org.jfrog.build.extractor.builder.ArtifactBuilder;
import org.jfrog.build.extractor.ci.Artifact;
import org.jfrog.build.extractor.ci.BuildInfoFields;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.UploadSpecHelper;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.jfrog.hudson.util.PropertyUtils;
import org.jfrog.hudson.util.SpecUtils;

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
            throws IOException, InterruptedException {
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

        public List<Artifact> invoke(File workspace, VirtualChannel channel) throws IOException {
            Log log = new JenkinsBuildInfoLog(listener);

            // Create ArtifactoryManagerBuilder
            ArtifactoryManagerBuilder artifactoryManagerBuilder = server.createArtifactoryManagerBuilder(credentials, proxyConfiguration, log);

            // Option 1. Upload - Use file specs.
            if (StringUtils.isNotEmpty(spec)) {
                SpecsHelper specsHelper = new SpecsHelper(log);
                try {
                    return specsHelper.uploadArtifactsBySpec(spec, threads, workspace, buildProperties, artifactoryManagerBuilder);
                } catch (Exception e) {
                    throw new RuntimeException("Failed uploading artifacts by spec", e);
                }
            }

            // Option 2. Generic deploy - Fetch the artifacts details from workspace by using 'patternPairs'.
            Set<DeployDetails> artifactsToDeploy = Sets.newHashSet();
            Multimap<String, File> targetPathToFilesMap = buildTargetPathToFiles(workspace);
            for (Map.Entry<String, File> entry : targetPathToFilesMap.entries()) {
                artifactsToDeploy.addAll(buildDeployDetailsFromFileEntry(entry));
            }
            try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
                deploy(artifactoryManager, artifactsToDeploy);
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

        public void deploy(ArtifactoryManager artifactoryManager, Set<DeployDetails> artifactsToDeploy)
                throws IOException {
            for (DeployDetails deployDetail : artifactsToDeploy) {
                artifactoryManager.upload(deployDetail);
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
                    .addProperties(buildProperties)
                    .packageType(DeployDetails.PackageType.GENERIC);
            result.add(builder.build());

            return result;
        }

        public enum PatternType {
            ANT, WILDCARD
        }
    }
}
