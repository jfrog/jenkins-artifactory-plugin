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

package org.jfrog.hudson.release.maven;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.ModuleName;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.hudson.ArtifactoryRedeployPublisher;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.PluginSettings;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.release.PromotionConfig;
import org.jfrog.hudson.release.ReleaseAction;
import org.jfrog.hudson.release.VcsConfig;
import org.jfrog.hudson.release.VersionedModule;
import org.jfrog.hudson.release.scm.svn.SubversionManager;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc} A release action which relates to Maven projects. All relevant information is taken from {@link
 * MavenModuleSet}
 *
 * @author Tomer Cohen
 */
public abstract class BaseMavenReleaseAction extends ReleaseAction<MavenModuleSet, MavenReleaseWrapper> {

    /**
     * Map of release versions per module. Only used if versioning is per module
     */
    private Map<ModuleName, String> releaseVersionPerModule;
    /**
     * Map of dev versions per module. Only used if versioning is per module
     */
    private Map<ModuleName, String> nextVersionPerModule;

    public BaseMavenReleaseAction(MavenModuleSet project) {
        super(project, MavenReleaseWrapper.class);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getDefaultVersioning() {
        return defaultVersioning;
    }

    @Override
    public List<String> getRepositoryKeys() throws IOException {
        ArtifactoryRedeployPublisher artifactoryPublisher = getPublisher();
        if (artifactoryPublisher != null) {
            return artifactoryPublisher.getArtifactoryServer().getReleaseRepositoryKeysFirst(getPublisher(), project);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isArtifactoryPro() {
        return getArtifactoryServer().isArtifactoryPro(getPublisher(), project);
    }

    @Override
    public ArtifactoryServer getArtifactoryServer() {
        ArtifactoryRedeployPublisher artifactoryPublisher = getPublisher();
        if (artifactoryPublisher != null) {
            return artifactoryPublisher.getArtifactoryServer();
        }
        return null;
    }

    @Override
    protected PluginSettings getSelectedStagingPlugin() throws Exception {
        return getPublisher().getSelectedStagingPlugin();
    }

    @Override
    protected String getSelectedStagingPluginName() {
        return getPublisher().getDeployerDetails().getUserPluginKey();
    }

    @Override
    protected void doPerModuleVersioning(StaplerRequest req) {
        releaseVersionPerModule = new HashMap<>();
        nextVersionPerModule = new HashMap<>();
        Enumeration params = req.getParameterNames();
        while (params.hasMoreElements()) {
            String key = (String) params.nextElement();
            if (key.startsWith("release.")) {
                ModuleName moduleName = ModuleName.fromString(StringUtils.removeStart(key, "release."));
                releaseVersionPerModule.put(moduleName, req.getParameter(key));
            } else if (key.startsWith("next.")) {
                ModuleName moduleName = ModuleName.fromString(StringUtils.removeStart(key, "next."));
                nextVersionPerModule.put(moduleName, req.getParameter(key));
            }
        }
    }

    @Override
    protected void doPerModuleVersioning(Map<String, VersionedModule> defaultModules) {
        releaseVersionPerModule = new HashMap<>();
        nextVersionPerModule = new HashMap<>();

        for (Map.Entry<String, VersionedModule> entry : defaultModules.entrySet()) {
            VersionedModule versionedModule = entry.getValue();
            ModuleName module = ModuleName.fromString(versionedModule.getModuleName());

            releaseVersionPerModule.put(module, versionedModule.getReleaseVersion());
            nextVersionPerModule.put(module, versionedModule.getNextDevelopmentVersion());
        }
    }

    @Override
    public String getReleaseVersionFor(Object moduleName) {
        ModuleName mavenModuleName = (ModuleName) moduleName;
        switch (versioning) {
            case GLOBAL:
                return releaseVersion;
            case PER_MODULE:
                return releaseVersionPerModule.get(mavenModuleName);
            default:
                return null;
        }
    }

    @Override
    public String getNextVersionFor(Object moduleName) {
        ModuleName mavenModuleName = (ModuleName) moduleName;
        switch (versioning) {
            case GLOBAL:
                return nextVersion;
            case PER_MODULE:
                return nextVersionPerModule.get(mavenModuleName);
            default:
                return null;
        }
    }

    public String getCurrentVersion() {
        return getRootModule().getVersion();
    }

    @Override
    protected void prepareBuilderSpecificDefaultVersioning() {
        defaultVersioning = getWrapper().getDefaultVersioning();
    }

    @Override
    protected void prepareBuilderSpecificDefaultGlobalModule() {
        if ((project != null) && (getRootModule() != null)) {
            String releaseVersion = calculateReleaseVersion(getRootModule().getVersion());
            defaultGlobalModule = new VersionedModule(null, releaseVersion, calculateNextVersion(releaseVersion));
        }
    }

    @Override
    protected void prepareBuilderSpecificDefaultModules() {
        defaultModules = new HashMap<>();
        if (project != null) {
            List<MavenModule> modules = project.getDisabledModules(false);
            for (MavenModule mavenModule : modules) {
                String version = mavenModule.getVersion();
                String moduleName = mavenModule.getModuleName().toString();
                defaultModules.put(moduleName, new VersionedModule(moduleName, calculateReleaseVersion(version),
                        calculateNextVersion(version)));
            }
        }
    }

    @Override
    protected void prepareBuilderSpecificDefaultVcsConfig() {
        String defaultReleaseBranch = getDefaultReleaseBranch();
        String defaultTagUrl = getDefaultTagUrl();
        defaultVcsConfig = new VcsConfig(StringUtils.isNotBlank(defaultReleaseBranch) && getWrapper().isUseReleaseBranch(), defaultReleaseBranch,
                StringUtils.isNotBlank(defaultTagUrl), defaultTagUrl, getDefaultTagComment(),
                getDefaultNextDevelCommitMessage());
    }

    @Override
    protected void prepareBuilderSpecificDefaultPromotionConfig() throws IOException {
        defaultPromotionConfig = new PromotionConfig(getDefaultReleaseStagingRepository(), null);
    }

    private MavenModule getRootModule() {
        return project.getRootModule();
    }

    private ArtifactoryRedeployPublisher getPublisher() {
        return ActionableHelper.getPublisher(project, ArtifactoryRedeployPublisher.class);
    }

    private String getDefaultReleaseBranch() {
        MavenReleaseWrapper wrapper = getWrapper();
        String releaseBranchPrefix = wrapper.getReleaseBranchPrefix();
        StringBuilder sb = new StringBuilder(StringUtils.trimToEmpty(releaseBranchPrefix));
        MavenModule rootModule = getRootModule();
        if (rootModule != null) {
            sb.append(rootModule.getModuleName().artifactId).append("-").append(getDefaultReleaseVersion());
        }
        return sb.toString();
    }

    private String getDefaultTagUrl() {
        MavenReleaseWrapper wrapper = getWrapper();
        String baseTagUrl = wrapper.getTagPrefix();
        StringBuilder sb = new StringBuilder(getBaseTagUrlAccordingToScm(baseTagUrl));
        MavenModule rootModule = getRootModule();
        if (rootModule != null) {
            sb.append(rootModule.getModuleName().artifactId).append("-").append(getDefaultReleaseVersion());
        }
        return sb.toString();
    }

    public String getTargetRemoteName() {
        return getWrapper().getTargetRemoteName();
    }

    private String getDefaultTagComment() {
        return SubversionManager.COMMENT_PREFIX + "Release version " + getDefaultReleaseVersion();
    }

    private String getDefaultReleaseVersion() {
        if (VERSIONING.GLOBAL.name().equals(getDefaultVersioning())) {
            return getDefaultGlobalReleaseVersion();
        } else {
            if (!defaultModules.isEmpty()) {
                return defaultModules.values().iterator().next().getReleaseVersion();
            }
        }
        return "";
    }

    private String getDefaultReleaseStagingRepository() throws IOException {
        //Get default staging repo from configuration.
        String defaultStagingRepo = getWrapper().getDefaultReleaseStagingRepository();
        if (!defaultStagingRepo.isEmpty() && getRepositoryKeys().contains(defaultStagingRepo)) {
            return defaultStagingRepo;
        }

        ArtifactoryRedeployPublisher publisher = getPublisher();
        if (publisher == null) {
            return null;
        }
        return publisher.getRepositoryKey();
    }
}
