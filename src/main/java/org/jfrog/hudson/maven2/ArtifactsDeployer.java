/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.maven2;

import hudson.EnvVars;
import hudson.Util;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.MavenArtifact;
import hudson.maven.reporters.MavenArtifactRecord;
import hudson.model.*;
import hudson.util.VersionNumber;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.ExtractorUtils;
import org.jfrog.hudson.util.IncludesExcludes;
import org.jfrog.hudson.util.MavenVersionHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Deploys artifacts to Artifactory. This class is used only when the Maven 3 extractor is not active.
 *
 * @author Yossi Shaul
 */
public class ArtifactsDeployer {
    private static final String HIGHEST_VERSION_BEFORE_ARCHIVE_FIX = "1.404";
    private static final String SHA1 = "SHA1";
    private static Logger debuggingLogger = Logger.getLogger(ArtifactsDeployer.class.getName());
    private final ArtifactoryServer artifactoryServer;
    private final String targetReleasesRepository;
    private final String targetSnapshotsRepository;
    private final ArtifactoryBuildInfoClient client;
    private final MavenModuleSetBuild mavenModuleSetBuild;
    private final BuildListener listener;
    private final IncludeExcludePatterns patterns;
    private final boolean downstreamIdentifier;
    private final boolean isArchiveJenkinsVersion;
    private final EnvVars env;
    private final String[] deploymentProperties;
    private final AbstractBuild<?, ?> rootBuild;

    public ArtifactsDeployer(ArtifactoryRedeployPublisher artifactoryPublisher, ArtifactoryBuildInfoClient client,
                             MavenModuleSetBuild mavenModuleSetBuild, BuildListener listener)
            throws IOException, InterruptedException {
        this.client = client;
        this.mavenModuleSetBuild = mavenModuleSetBuild;
        this.listener = listener;
        this.env = mavenModuleSetBuild.getEnvironment(listener);
        this.artifactoryServer = artifactoryPublisher.getArtifactoryServer();
        // release action might change the target releases repository
        ReleaseAction releaseAction = ActionableHelper.getLatestAction(mavenModuleSetBuild, ReleaseAction.class);
        if (releaseAction != null) {
            String stagingRepoKey = releaseAction.getStagingRepositoryKey();
            if (StringUtils.isBlank(stagingRepoKey)) {
                stagingRepoKey = Util.replaceMacro(artifactoryPublisher.getRepositoryKey(), env);
            }
            this.targetReleasesRepository = stagingRepoKey;
        } else {
            this.targetReleasesRepository = Util.replaceMacro(artifactoryPublisher.getRepositoryKey(), env);
        }
        this.targetSnapshotsRepository = Util.replaceMacro(artifactoryPublisher.getSnapshotsRepositoryKey(), env);
        this.downstreamIdentifier = artifactoryPublisher.isPassIdentifiedDownstream();
        IncludesExcludes patterns = artifactoryPublisher.getArtifactDeploymentPatterns();
        if (patterns != null) {
            this.patterns = new IncludeExcludePatterns(
                    Util.replaceMacro(patterns.getIncludePatterns(), env),
                    Util.replaceMacro(patterns.getExcludePatterns(), env)
            );
        } else {
            this.patterns = IncludeExcludePatterns.EMPTY;
        }
        this.deploymentProperties = StringUtils.split(Util.replaceMacro(artifactoryPublisher.getDeploymentProperties(), env), "; ");
        debuggingLogger.fine("Getting root build");
        this.rootBuild = BuildUniqueIdentifierHelper.getRootBuild(mavenModuleSetBuild);
        this.isArchiveJenkinsVersion = Hudson.getVersion().isNewerThan(new VersionNumber(
                HIGHEST_VERSION_BEFORE_ARCHIVE_FIX));
    }

    public void deploy() throws IOException, InterruptedException, NoSuchAlgorithmException {
        listener.getLogger().println("Deploying artifacts to " + artifactoryServer.getArtifactoryUrl());
        Map<MavenModule, MavenBuild> mavenBuildMap = mavenModuleSetBuild.getModuleLastBuilds();

        for (Map.Entry<MavenModule, MavenBuild> mavenBuildEntry : mavenBuildMap.entrySet()) {
            MavenBuild mavenBuild = mavenBuildEntry.getValue();
            Result result = mavenBuild.getResult();
            if (Result.NOT_BUILT.equals(result)) {
                // HAP-52 - the module build might be skipped if using incremental build
                listener.getLogger().println(
                        "Module: '" + mavenBuildEntry.getKey().getName() + "' wasn't built. Skipping.");
                continue;
            }
            listener.getLogger().println("Deploying artifacts of module: " + mavenBuildEntry.getKey().getName());
            MavenArtifactRecord mar = ActionableHelper.getLatestMavenArtifactRecord(mavenBuild);
            MavenArtifact mavenArtifact = mar.mainArtifact;

            // deploy main artifact
            debuggingLogger.fine("Deploying main artifact: " + artifactToString(mavenArtifact, mavenBuild));
            deployArtifact(mavenBuild, mavenArtifact);
            if (!mar.isPOM() && mar.pomArtifact != null && mar.pomArtifact != mar.mainArtifact) {
                // deploy the pom if the main artifact is not the pom
                debuggingLogger.fine("Deploying pom artifact: " + artifactToString(mavenArtifact, mavenBuild));
                deployArtifact(mavenBuild, mar.pomArtifact);
            }

            // deploy attached artifacts
            for (MavenArtifact attachedArtifact : mar.attachedArtifacts) {
                debuggingLogger.fine("Deploying attached artifact: " + artifactToString(mavenArtifact, mavenBuild));
                deployArtifact(mavenBuild, attachedArtifact);
            }
        }
    }

    private String artifactToString(MavenArtifact mavenArtifact, MavenBuild mavenBuild) throws IOException {
        return new StringBuilder().append(ToStringBuilder.reflectionToString(mavenArtifact))
                .append("[File: ").append(getArtifactFile(mavenBuild, mavenArtifact)).append("]")
                .toString();
    }

    private void deployArtifact(MavenBuild mavenBuild, MavenArtifact mavenArtifact)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        String artifactPath = buildArtifactPath(mavenArtifact);

        if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
            listener.getLogger().println("Skipping the deployment of '" + artifactPath +
                    "' due to the defined include-exclude patterns.");
            return;
        }

        File artifactFile = getArtifactFile(mavenBuild, mavenArtifact);
        // calculate the sha1 checksum that is not given by Jenkins and add it to the deploy details
        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifactFile, SHA1);
        DeployDetails.Builder builder = new DeployDetails.Builder()
                .file(artifactFile)
                .artifactPath(artifactPath)
                .targetRepository(getTargetRepository(mavenArtifact.version))
                .md5(mavenArtifact.md5sum).sha1(checksums.get(SHA1))
                .addProperty(BuildInfoFields.BUILD_NAME, ExtractorUtils.sanitizeBuildName(mavenModuleSetBuild.getParent().getFullName()))
                .addProperty(BuildInfoFields.BUILD_NUMBER, mavenModuleSetBuild.getNumber() + "")
                .addProperty(BuildInfoFields.BUILD_TIMESTAMP, mavenBuild.getTimestamp().getTime().getTime() + "");

        String identifier = BuildUniqueIdentifierHelper.getUpstreamIdentifier(rootBuild);
        if (StringUtils.isNotBlank(identifier)) {
            builder.addProperty(BuildInfoFields.BUILD_ROOT, identifier);
        }

        Cause.UpstreamCause parent = ActionableHelper.getUpstreamCause(mavenModuleSetBuild);
        if (parent != null) {
            builder.addProperty(BuildInfoFields.BUILD_PARENT_NAME, ExtractorUtils.sanitizeBuildName(parent.getUpstreamProject()))
                    .addProperty(BuildInfoFields.BUILD_PARENT_NUMBER, parent.getUpstreamBuild() + "");
        }
        String revision = ExtractorUtils.getVcsRevision(env);
        if (StringUtils.isNotBlank(revision)) {
            builder.addProperty(BuildInfoFields.VCS_REVISION, revision);
        }
        String url = ExtractorUtils.getVcsUrl(env);
        if (StringUtils.isNotBlank(url)) {
            builder.addProperty(BuildInfoFields.VCS_URL, url);
        }
        addDeploymentProperties(builder);
        DeployDetails deployDetails = builder.build();
        logDeploymentPath(deployDetails, artifactPath);
        client.deployArtifact(deployDetails);
    }

    private void addDeploymentProperties(DeployDetails.Builder builder) {
        if (deploymentProperties == null) {
            return;
        }
        for (String property : deploymentProperties) {
            String[] split = StringUtils.split(property, '=');
            if (split.length == 2) {
                String value = split[1];
                builder.addProperty(split[0], value);
            }
        }
    }

    private void logDeploymentPath(DeployDetails deployDetails, String artifactPath) {
        String deploymentPath =
                artifactoryServer.getArtifactoryUrl() + "/" + deployDetails.getTargetRepository() + "/" + artifactPath;
        listener.getLogger().println("Deploying artifact: " + deploymentPath);
    }

    /**
     * @return Return the target deployment repository. Either the releases repository (default) or snapshots if defined
     * and the deployed version is a snapshot.
     */
    public String getTargetRepository(String version) {
        if (targetSnapshotsRepository != null && version.endsWith("SNAPSHOT")) {
            return targetSnapshotsRepository;
        }
        return targetReleasesRepository;
    }

    private String buildArtifactPath(MavenArtifact mavenArtifact) {
        String directoryPath =
                mavenArtifact.groupId.replace('.', '/') + "/" + mavenArtifact.artifactId + "/" + mavenArtifact.version;
        return directoryPath + "/" + mavenArtifact.canonicalName;
    }

    /**
     * Obtains the {@link java.io.File} representing the archived artifact.
     */
    private File getArtifactFile(MavenBuild build, MavenArtifact mavenArtifact) throws IOException {
        String fileName = mavenArtifact.fileName;
        if (isArchiveJenkinsVersion) {
            fileName = mavenArtifact.canonicalName;
        }
        File file = new File(new File(new File(new File(build.getArtifactsDir(), mavenArtifact.groupId),
                mavenArtifact.artifactId), mavenArtifact.version), fileName);
        if (!file.exists()) {
            throw new FileNotFoundException("Archived artifact is missing: " + file + " " + getAdditionalMessage());
        }
        return file;
    }

    /**
     * @return An additional error message to be attached to the exception
     */
    private String getAdditionalMessage() throws IOException {
        try {
            if (MavenVersionHelper.isLowerThanMaven3(mavenModuleSetBuild, env, listener)) {
                return "\nDisabling the automatic archiving and using the external Maven extractor is compatible with Maven 3.0.2 and up";
            }
            return "";
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to determine Maven version", e);
        }
    }
}
