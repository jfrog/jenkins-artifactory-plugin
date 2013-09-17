/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.hudson.maven3.extractor;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.remoting.Which;
import hudson.scm.NullChangeLogParser;
import hudson.scm.NullSCM;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.maven.BuildInfoRecorder;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.ServerDetails;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.maven3.ArtifactoryMaven3NativeConfigurator;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.util.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

/**
 * Class for setting up the {@link Environment} for a {@link MavenModuleSet} project. Responsible for adding the new
 * maven opts with the location of the plugin.
 *
 * @author Tomer Cohen
 */
public class MavenExtractorEnvironment extends Environment {
    public static final String MAVEN_PLUGIN_OPTS = "-Dm3plugin.lib";
    public static final String CLASSWORLDS_CONF_KEY = "classworlds.conf";

    private final MavenModuleSet project;
    private final String originalMavenOpts;
    private final ArtifactoryRedeployPublisher publisher;
    private final MavenModuleSetBuild build;
    private final ArtifactoryMaven3NativeConfigurator resolver;
    private final BuildListener buildListener;
    private final EnvVars envVars;
    private FilePath classworldsConf;
    private String propertiesFilePath;

    // the build env vars method may be called again from another setUp of a wrapper so we need this flag to
    // attempt only once certain operations (like copying file or changing maven opts).
    private boolean initialized;

    public MavenExtractorEnvironment(MavenModuleSetBuild build, ArtifactoryRedeployPublisher publisher,
                                     ArtifactoryMaven3NativeConfigurator resolver, BuildListener buildListener)
            throws IOException, InterruptedException {
        this.buildListener = buildListener;
        this.project = build.getProject();
        this.build = build;
        this.publisher = publisher;
        this.resolver = resolver;
        this.originalMavenOpts = project.getMavenOpts();
        this.envVars = build.getEnvironment(buildListener);
    }

    @Override
    public void buildEnvVars(Map<String, String> env) {

        if (build.getWorkspace() == null) {
            // HAP-274 - workspace might not be initialized yet (this method will be called later in the build lifecycle)
            return;
        }

        //If an SCM is configured
        if (!initialized && !(build.getProject().getScm() instanceof NullSCM)) {
            //Handle all the extractor info only when a checkout was already done
            boolean checkoutWasPerformed = true;
            try {
                Field scmField = AbstractBuild.class.getDeclaredField("scm");
                scmField.setAccessible(true);
                Object scmObject = scmField.get(build);
                //Null changelog parser is set when a checkout wasn't performed yet
                checkoutWasPerformed = !(scmObject instanceof NullChangeLogParser);
            } catch (Exception e) {
                buildListener.getLogger().println("[Warning] An error occurred while testing if the SCM checkout " +
                        "has already been performed: " + e.getMessage());
            }
            if (!checkoutWasPerformed) {
                return;
            }
        }

        // if not valid Maven version don't modify the environment
        if (!isMavenVersionValid()) {
            return;
        }
        env.put(ExtractorUtils.EXTRACTOR_USED, "true");

        if (classworldsConf == null && !env.containsKey(CLASSWORLDS_CONF_KEY)) {
            URL resource = getClass().getClassLoader().getResource("org/jfrog/hudson/maven3/classworlds-native.conf");
            classworldsConf = copyClassWorldsFile(build, resource);
        }

        if (classworldsConf != null) {
            addCustomClassworlds(env, classworldsConf.getRemote());
        }

        if (!initialized) {
            try {
                build.getProject().setMavenOpts(appendNewMavenOpts(project, build, buildListener));

                PublisherContext publisherContext = null;
                if (publisher != null) {
                    publisherContext = createPublisherContext(publisher, build);
                }

                ResolverContext resolverContext = null;
                if (resolver != null) {
                    Credentials resolverCredentials = CredentialResolver.getPreferredResolver(
                            resolver, resolver.getArtifactoryServer());
                    resolverContext = new ResolverContext(resolver.getArtifactoryServer(), resolver.getDetails(),
                            resolverCredentials);
                }

                ArtifactoryClientConfiguration configuration = ExtractorUtils.addBuilderInfoArguments(
                        env, build, buildListener, publisherContext, resolverContext);
                propertiesFilePath = configuration.getPropertiesFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            initialized = true;
        }

        env.put(BuildInfoConfigProperties.PROP_PROPS_FILE, propertiesFilePath);
    }

    private boolean isMavenVersionValid() {
        try {
            return MavenVersionHelper.isAtLeastResolutionCapableVersion(build, envVars, buildListener);
        } catch (Exception e) {
            throw new RuntimeException("Unable to determine Maven version", e);
        }
    }

    @Override
    public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
        project.setMavenOpts(originalMavenOpts);
        if (classworldsConf != null) {
            classworldsConf.delete();
        }
        return true;
    }

    private PublisherContext createPublisherContext(ArtifactoryRedeployPublisher publisher, AbstractBuild build) {
        ReleaseAction release = ActionableHelper.getLatestAction(build, ReleaseAction.class);
        ServerDetails server = publisher.getDetails();
        if (release != null) {
            // staging build might change the target deployment repository
            String stagingRepoKey = release.getStagingRepositoryKey();
            if (!StringUtils.isBlank(stagingRepoKey) && !stagingRepoKey.equals(server.repositoryKey)) {
                server = new ServerDetails(server.artifactoryName, server.getArtifactoryUrl(), stagingRepoKey,
                        server.snapshotsRepositoryKey, server.downloadRepositoryKey);
            }
        }

        PublisherContext context = new PublisherContext.Builder().artifactoryServer(publisher.getArtifactoryServer())
                .serverDetails(server).deployerOverrider(publisher).runChecks(publisher.isRunChecks())
                .includePublishArtifacts(publisher.isIncludePublishArtifacts())
                .violationRecipients(publisher.getViolationRecipients()).scopes(publisher.getScopes())
                .licenseAutoDiscovery(publisher.isLicenseAutoDiscovery())
                .discardOldBuilds(publisher.isDiscardOldBuilds()).deployArtifacts(publisher.isDeployArtifacts())
                .includesExcludes(publisher.getArtifactDeploymentPatterns())
                .skipBuildInfoDeploy(!publisher.isDeployBuildInfo())
                .includeEnvVars(publisher.isIncludeEnvVars()).envVarsPatterns(publisher.getEnvVarsPatterns())
                .discardBuildArtifacts(publisher.isDiscardBuildArtifacts())
                .matrixParams(publisher.getMatrixParams()).evenIfUnstable(publisher.isEvenIfUnstable())
                .enableIssueTrackerIntegration(publisher.isEnableIssueTrackerIntegration())
                .aggregateBuildIssues(publisher.isAggregateBuildIssues())
                .aggregationBuildStatus(publisher.getAggregationBuildStatus())
                .integrateBlackDuck(publisher.isBlackDuckRunChecks(), publisher.getBlackDuckAppName(),
                        publisher.getBlackDuckAppVersion(), publisher.getBlackDuckReportRecipients(),
                        publisher.getBlackDuckScopes(), publisher.isBlackDuckIncludePublishedArtifacts(),
                        publisher.isAutoCreateMissingComponentRequests(),
                        publisher.isAutoDiscardStaleComponentRequests())
                .build();

        return context;
    }


    /**
     * Append custom Maven opts to the existing to the already existing ones. The opt that will be appended is the
     * location Of the plugin for the Maven process to use.
     */
    public String appendNewMavenOpts(MavenModuleSet project, AbstractBuild build, BuildListener listener)
            throws IOException {
        String opts = project.getMavenOpts();

        if (StringUtils.contains(opts, MAVEN_PLUGIN_OPTS)) {
            listener.getLogger().println(
                    "Property '" + MAVEN_PLUGIN_OPTS +
                            "' is already part of MAVEN_OPTS. This is usually a leftover of " +
                            "previous build which was forcibly stopped. Replacing the value with an updated one. " +
                            "Please remove it from the job configuration.");
            // this regex will remove the property and the value (the value either ends with a space or surrounded by quotes
            opts = opts.replaceAll(MAVEN_PLUGIN_OPTS + "=([^\\s\"]+)|" + MAVEN_PLUGIN_OPTS + "=\"([^\"]*)\"", "")
                    .trim();
        }

        StringBuilder mavenOpts = new StringBuilder();
        if (StringUtils.isNotBlank(opts)) {
            mavenOpts.append(opts);
        }

        File maven3ExtractorJar = Which.jarFile(BuildInfoRecorder.class);
        try {
            FilePath actualDependencyDirectory =
                    PluginDependencyHelper.getActualDependencyDirectory(build, maven3ExtractorJar);
            mavenOpts.append(" ").append(MAVEN_PLUGIN_OPTS).append("=")
                    .append(quote(actualDependencyDirectory.getRemote()))
                    .append(" -Dmaven3.interceptor.common=").append(quote(actualDependencyDirectory.getRemote()));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return mavenOpts.toString();
    }

    /**
     * Adds quotes around strings containing spaces.
     */
    private static String quote(String arg) {

        if (StringUtils.isNotBlank(arg) && arg.indexOf(' ') >= 0) {
            return "\"" + arg + "\"";
        } else {
            return arg;
        }
    }

    /**
     * Copies a classworlds file to a temporary location either on the local filesystem or on a slave depending on the
     * node type.
     *
     * @return The path of the classworlds.conf file
     */
    private FilePath copyClassWorldsFile(AbstractBuild<?, ?> build, URL resource) {
        try {
            FilePath remoteClassworlds =
                    build.getWorkspace().createTextTempFile("classworlds", "conf", "", false);
            remoteClassworlds.copyFrom(resource);
            return remoteClassworlds;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add a custom {@code classworlds.conf} file that will be read by the Maven build. Adds an environment variable
     * {@code classwordls.conf} with the location of the classworlds file for Maven.
     *
     * @return The path of the classworlds.conf file
     */
    public static void addCustomClassworlds(Map<String, String> env, String classworldsConfPath) {
        env.put(CLASSWORLDS_CONF_KEY, classworldsConfPath);
    }
}
