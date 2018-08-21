/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.hudson.release.gradle;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.plugins.gradle.Gradle;
import hudson.tasks.Builder;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.PluginSettings;
import org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator;
import org.jfrog.hudson.release.PromotionConfig;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.release.VcsConfig;
import org.jfrog.hudson.release.VersionedModule;
import org.jfrog.hudson.release.scm.svn.SubversionManager;
import org.jfrog.hudson.util.PropertyUtils;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.*;

/**
 * {@inheritDoc} A release action which relates to Maven projects. All relevant information is taken from the {@code
 * gradle.properties} file which is related to the Gradle build.
 *
 * @author Tomer Cohen
 */
public abstract class BaseGradleReleaseAction extends ReleaseAction<AbstractProject<?, ?>, ArtifactoryGradleConfigurator> {

    private transient Map<String, String> releaseProps;
    private transient Map<String, String> nextIntegProps;
    /**
     * Map of release versions per module. Only used if versioning is per module
     */
    private Map<String, String> releaseVersionPerModule;
    /**
     * Map of dev versions per module. Only used if versioning is per module
     */
    private Map<String, String> nextVersionPerModule;

    public BaseGradleReleaseAction(AbstractProject<?, ?> project) {
        super(project, ArtifactoryGradleConfigurator.class);
    }

    public String[] getReleaseProperties() {
        return getReleaseWrapper().getReleasePropsKeysList();
    }

    public String[] getNextIntegProperties() {
        return getReleaseWrapper().getNextIntegPropsKeysList();
    }

    /**
     * Initialize the version properties map from the gradle.properties file, and the additional properties from the
     * gradle.properties file.
     */
    @Override
    protected void initBuilderSpecific() throws Exception {
        reset();
        FilePath workspace = getModuleRoot(EnvVars.masterEnvVars);
        FilePath gradlePropertiesPath = new FilePath(workspace, "gradle.properties");
        if (releaseProps == null) {
            releaseProps = PropertyUtils.getModulesPropertiesFromPropFile(gradlePropertiesPath, getReleaseProperties());
        }
        if (nextIntegProps == null) {
            nextIntegProps =
                    PropertyUtils.getModulesPropertiesFromPropFile(gradlePropertiesPath, getNextIntegProperties());
        }
    }

    /**
     * Get the root path where the build is located, the project may be checked out to
     * a sub-directory from the root workspace location.
     *
     * @param globalEnv EnvVars to take the workspace from, if workspace is not found
     *                  then it is take from project.getSomeWorkspace()
     * @return The location of the root of the Gradle build.
     * @throws IOException
     * @throws InterruptedException
     */
    public FilePath getModuleRoot(Map<String, String> globalEnv) throws IOException, InterruptedException {
        FilePath someWorkspace = project.getSomeWorkspace();
        if (someWorkspace == null) {
            throw new IllegalStateException("Couldn't find workspace");
        }

        Map<String, String> workspaceEnv = Maps.newHashMap();
        workspaceEnv.put("WORKSPACE", someWorkspace.getRemote());

        for (Builder builder : getBuilders()) {
            if (builder instanceof Gradle) {
                Gradle gradleBuilder = (Gradle) builder;
                String rootBuildScriptDir = gradleBuilder.getRootBuildScriptDir();
                if (rootBuildScriptDir != null && rootBuildScriptDir.trim().length() != 0) {
                    String rootBuildScriptNormalized = Util.replaceMacro(rootBuildScriptDir.trim(), workspaceEnv);
                    rootBuildScriptNormalized = Util.replaceMacro(rootBuildScriptNormalized, globalEnv);
                    return new FilePath(someWorkspace, rootBuildScriptNormalized);
                } else {
                    return someWorkspace;
                }
            }
        }

        throw new IllegalArgumentException("Couldn't find Gradle builder in the current builders list");
    }

    /**
     * Nullify the version properties map and the additional properties map, should be only called once the build is
     * <b>finished</b>. <p>Since the GradleReleaseAction is saved in memory and is only build when re-saving a project's
     * config or during startup, therefore a cleanup of the internal maps is needed.</p>
     */
    public void reset() {
        releaseProps = null;
        nextIntegProps = null;
        releaseVersion = null;
    }

    /**
     * @return List of target repositories for deployment (release repositories first). Called from the UI.
     */
    @Override
    @SuppressWarnings({"UnusedDeclaration"})
    public List<String> getRepositoryKeys() throws IOException {
        ArtifactoryServer server = getArtifactoryServer();
        if (server != null) {
            return getArtifactoryServer().getReleaseRepositoryKeysFirst(getWrapper(), project);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isArtifactoryPro() {
        return getArtifactoryServer().isArtifactoryPro(getWrapper(), project);
    }

    @Override
    public ArtifactoryServer getArtifactoryServer() {
        ArtifactoryGradleConfigurator configurator = getWrapper();
        if (configurator != null) {
            return configurator.getArtifactoryServer();
        }
        return null;
    }

    @Override
    public String getTargetRemoteName() {
        return getReleaseWrapper().getTargetRemoteName();
    }

    @Override
    public String latestVersioningSelection() {
        return VERSIONING.PER_MODULE.name();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getValueForProp(String prop) {
        return nextIntegProps.get(prop);
    }

    @Override
    public String calculateReleaseVersion(String fromVersion) {
        String version = releaseProps.get(fromVersion);
        if (StringUtils.isBlank(version)) {
            version = nextIntegProps.get(fromVersion);
        }
        if (StringUtils.isNotBlank(version)) {
            return super.calculateReleaseVersion(version);
        }
        return "";
    }

    public String getCurrentVersionFor(String moduleName) {
        return releaseProps.get(moduleName);
    }

    @Override
    public String getReleaseVersionFor(Object moduleName) {
        switch (versioning) {
            case GLOBAL:
                return releaseVersion;
            case PER_MODULE:
                return releaseVersionPerModule.get(moduleName.toString());
            default:
                return null;
        }
    }

    @Override
    public String getNextVersionFor(Object moduleName) {
        switch (versioning) {
            case GLOBAL:
                return nextVersion;
            case PER_MODULE:
                return nextVersionPerModule.get(moduleName.toString());
            default:
                return null;
        }
    }

    @Override
    protected PluginSettings getSelectedStagingPlugin() throws Exception {
        return getWrapper().getSelectedStagingPlugin();
    }

    @Override
    protected String getSelectedStagingPluginName() {
        return getWrapper().getDeployerDetails().getUserPluginKey();
    }

    @Override
    protected void doPerModuleVersioning(StaplerRequest req) {
        releaseVersionPerModule = Maps.newHashMap();
        nextVersionPerModule = Maps.newHashMap();
        Enumeration params = req.getParameterNames();
        while (params.hasMoreElements()) {
            String key = (String) params.nextElement();
            if (key.startsWith("release.")) {
                releaseVersionPerModule.put(StringUtils.removeStart(key, "release."), req.getParameter(key));
            } else if (key.startsWith("next.")) {
                nextVersionPerModule.put(StringUtils.removeStart(key, "next."), req.getParameter(key));
            }
        }
    }

    @Override
    protected void doPerModuleVersioning(Map<String, VersionedModule> defaultModules) {
        releaseVersionPerModule = Maps.newHashMap();
        nextVersionPerModule = Maps.newHashMap();

        for (Map.Entry<String, VersionedModule> entry : defaultModules.entrySet()) {
            VersionedModule versionedModule = entry.getValue();
            String module = versionedModule.getModuleName();
            releaseVersionPerModule.put(module, versionedModule.getReleaseVersion());
            nextVersionPerModule.put(module, versionedModule.getNextDevelopmentVersion());
        }
    }

    @Override
    protected void prepareBuilderSpecificDefaultGlobalModule() {
    }

    @Override
    protected void prepareBuilderSpecificDefaultVersioning() {
        defaultVersioning = VERSIONING.PER_MODULE.toString();
    }

    @Override
    protected void prepareBuilderSpecificDefaultModules() {
        defaultModules = Maps.newHashMap();

        for (String releaseProperties : getReleaseProperties()) {
            defaultModules.put(releaseProperties, new VersionedModule(releaseProperties,
                    calculateReleaseVersion(releaseProperties), null));
        }

        for (String nextIntegProperty : getNextIntegProperties()) {
            defaultModules.put(nextIntegProperty, new VersionedModule(nextIntegProperty,
                    calculateReleaseVersion(nextIntegProperty), calculateNextVersion(nextIntegProperty)));
        }
    }

    @Override
    protected void prepareBuilderSpecificDefaultVcsConfig() {
        String defaultReleaseBranch = getDefaultReleaseBranch();
        String defaultTagUrl = getDefaultTagUrl();
        defaultVcsConfig = new VcsConfig(StringUtils.isNotBlank(defaultReleaseBranch) && getReleaseWrapper().isUseReleaseBranch(),
                defaultReleaseBranch, StringUtils.isNotBlank(defaultTagUrl), defaultTagUrl,
                getDefaultTagComment(), getDefaultNextDevelCommitMessage());
    }

    @Override
    protected void prepareBuilderSpecificDefaultPromotionConfig() throws IOException {
        defaultPromotionConfig = new PromotionConfig(getDefaultReleaseStagingRepository(), null);
    }

    private GradleReleaseWrapper getReleaseWrapper() {
        return getWrapper().getReleaseWrapper();
    }

    private String getDefaultReleaseBranch() {
        String releaseBranchPrefix = getReleaseWrapper().getReleaseBranchPrefix();
        return new StringBuilder(StringUtils.trimToEmpty(releaseBranchPrefix)).append(getFirstReleaseVersion())
                .toString();
    }

    private String getDefaultTagUrl() {
        String baseTagUrl = getReleaseWrapper().getTagPrefix();
        return new StringBuilder(getBaseTagUrlAccordingToScm(baseTagUrl)).append(getFirstReleaseVersion()).toString();
    }

    private String getDefaultTagComment() {
        return new StringBuilder(SubversionManager.COMMENT_PREFIX).append("Release version ")
                .append(getFirstReleaseVersion()).toString();
    }

    private String getFirstReleaseVersion() {
        return super.calculateReleaseVersion(getCurrentVersion());
    }

    private String getCurrentVersion() {
        String version = extractNumericVersion(releaseProps.values());
        if (StringUtils.isBlank(version)) {
            version = extractNumericVersion(nextIntegProps.values());
        }
        if (StringUtils.isBlank(version)) {
            if (!releaseProps.values().isEmpty()) {
                version = releaseProps.values().iterator().next();
            } else if (!nextIntegProps.values().isEmpty()) {
                version = nextIntegProps.values().iterator().next();
            }
        }
        return version;
    }

    /**
     * Try to extract a numeric version from a collection of strings.
     *
     * @param versionStrings Collection of string properties.
     * @return The version string if exists in the collection.
     */
    private String extractNumericVersion(Collection<String> versionStrings) {
        if (versionStrings == null) {
            return "";
        }
        for (String value : versionStrings) {
            String releaseValue = calculateReleaseVersion(value);
            if (!releaseValue.equals(value)) {
                return releaseValue;
            }
        }
        return "";
    }

    /**
     * @return The release repository configured in Artifactory publisher.
     */
    private String getDefaultReleaseStagingRepository() throws IOException {
        // Get default staging repo from configuration.
        String defaultStagingRepo = getReleaseWrapper().getDefaultReleaseStagingRepository();
        if (defaultStagingRepo != null && getRepositoryKeys().contains(defaultStagingRepo)) {
            return defaultStagingRepo;
        }

        ArtifactoryGradleConfigurator publisher = getWrapper();
        if (publisher == null) {
            return null;
        }
        return publisher.getRepositoryKey();
    }
}
