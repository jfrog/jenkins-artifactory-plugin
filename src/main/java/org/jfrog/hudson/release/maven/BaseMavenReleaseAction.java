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

import com.google.common.collect.Maps;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.ModuleName;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;
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

import java.util.*;

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
    public List<String> getRepositoryKeys() {
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
        return getPublisher().getDetails().getUserPluginKey();
    }

    @Override
    protected void doPerModuleVersioning(StaplerRequest req) {
        Map<ModuleName, String> releaseVersionPerModule = Maps.newHashMap();
        Map<ModuleName, String> nextVersionPerModule = Maps.newHashMap();
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
        putMissingValuesRelease(releaseVersionPerModule, defaultModules);
        putMissingValuesNext(nextVersionPerModule, defaultModules);
    }

    private void putMissingValuesNext(Map<ModuleName, String> nextVersionPerModule, Map<String, VersionedModule> defaultModules) {

        // going throw the default modules.
        for (Map.Entry<String, VersionedModule> entry : defaultModules.entrySet()){

            String key = entry.getKey();
            String[] keySplit = key.split(":");
            ModuleName md = new ModuleName(keySplit[0], keySplit[1]);
            VersionedModule vm = entry.getValue();
            if(!isContains(nextVersionPerModule, md)){
                nextVersionPerModule.put(md, vm.getNextDevelopmentVersion());
            }
        }

        this.nextVersionPerModule = nextVersionPerModule;

    }

    private void putMissingValuesRelease(Map<ModuleName, String> releaseVersionPerModule, Map<String, VersionedModule> defaultModules) {
        // going throw the default modules.
        for (Map.Entry<String, VersionedModule> entry : defaultModules.entrySet()){

            String key = entry.getKey();
            String[] keySplit = key.split(":");
            ModuleName md = new ModuleName(keySplit[0], keySplit[1]);
            VersionedModule vm = entry.getValue();
            if(!isContains(releaseVersionPerModule, md)){
                releaseVersionPerModule.put(md, vm.getReleaseVersion());
            }
        }

        this.releaseVersionPerModule = releaseVersionPerModule;
    }

    private boolean isContains(Map<ModuleName, String> versionPerModule, ModuleName md) {
        //checking each default module if exists in the per module map
        for(Map.Entry<ModuleName, String> entry2 : versionPerModule.entrySet()){
            ModuleName md2 = entry2.getKey();
            if(md2.artifactId.equalsIgnoreCase(md.artifactId) && md2.groupId.equalsIgnoreCase(md.groupId)){
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doPerModuleVersioning(Map<String, VersionedModule> defaultModules) {
        releaseVersionPerModule = Maps.newHashMap();
        nextVersionPerModule = Maps.newHashMap();

        for(Map.Entry<String, VersionedModule> entry : defaultModules.entrySet()) {
            VersionedModule versionedModule  = entry.getValue();
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
        defaultModules = Maps.newHashMap();
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
        defaultVcsConfig = new VcsConfig(StringUtils.isNotBlank(defaultReleaseBranch), defaultReleaseBranch,
                StringUtils.isNotBlank(defaultTagUrl), defaultTagUrl, getDefaultTagComment(),
                getDefaultNextDevelCommitMessage());
    }

    @Override
    protected void prepareBuilderSpecificDefaultPromotionConfig() {
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
                defaultModules.values().iterator().next().getReleaseVersion();
            }
        }
        return "";
    }

    private String getDefaultReleaseStagingRepository() {
        //Get default staging repo from configuration.
        String defaultStagingRepo = getWrapper().getDefaultReleaseStagingRepository();
        if (defaultStagingRepo != null && getRepositoryKeys().contains(defaultStagingRepo)) {
            return defaultStagingRepo;
        }

        ArtifactoryRedeployPublisher publisher = getPublisher();
        if (publisher == null) {
            return null;
        }
        return publisher.getRepositoryKey();
    }


    @Override
    public boolean isValid(AbstractBuild build, BuildListener listener, String versioning){
        MavenModuleSetBuild mavenBuild = (MavenModuleSetBuild) build;
        if(checkQueryValidation(versioning, mavenBuild, listener))
            return true;
        else
            return false;
    }

    private boolean checkQueryValidation(String versioning, MavenModuleSetBuild mavenBuild, BuildListener listener) {

        if(versioning.equals(ReleaseAction.VERSIONING.NONE.name()))
            return true;

        if(versioning.equals(ReleaseAction.VERSIONING.PER_MODULE.name())){
            return checkValidationPerModule(mavenBuild, listener);
        }

        if(versioning.equals(ReleaseAction.VERSIONING.GLOBAL.name()))
            return checkValidationGLOBAL(listener);


        return true;
    }

    private boolean checkValidationGLOBAL(BuildListener listener) {

        if(StringUtils.isEmpty(releaseVersion) || StringUtils.isEmpty(nextVersion)){
            log(listener, "ERROR: No release version or next version is configured.");
            return false;
        }
        return true;
    }

    private boolean checkValidationPerModule(MavenModuleSetBuild mavenBuild, BuildListener listener) {

        if(releaseVersionPerModule == null || nextVersionPerModule == null){
            log(listener, "ERROR: No release version or next version is configured.");
            return false;
        }

        if(releaseVersionPerModule.size() < mavenBuild.getModuleBuilds().size() || nextVersionPerModule.size() < mavenBuild.getModuleBuilds().size()){
            log(listener, "ERROR: Not all the modules are configured with version for release or next development.");
            return false;
        }

        Iterator it = mavenBuild.getProject().getModules().iterator();
        while( it.hasNext() ){
            MavenModule key = (MavenModule) it.next();
            ModuleName moduleName = key.getModuleName();
            if( releaseVersionPerModule.get(moduleName) == null || nextVersionPerModule.get(moduleName) == null ){
                log(listener, "ERROR: There is missing GropuID " + moduleName.groupId + " or ArtifactID " + moduleName.artifactId + " for the release/next version.");
                return false;
            }
        }

        return true;
    }

    private void log(BuildListener listener, String message) {
        listener.getLogger().println("[RELEASE] " + message);
    }



}
