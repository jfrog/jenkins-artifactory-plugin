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

package org.jfrog.hudson.release;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Maps;
import hudson.Util;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.ArtifactoryPlugin;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.PluginSettings;
import org.jfrog.hudson.UserPluginInfo;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.release.scm.AbstractScmCoordinator;
import org.jfrog.hudson.release.scm.svn.SubversionManager;
import org.jfrog.hudson.util.ErrorResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This action leads to execution of the release wrapper. It will collect information from the user about the release
 * and will trigger the release build. This action is not saved in the job xml file.
 *
 * @author Yossi Shaul
 */
public abstract class ReleaseAction<P extends AbstractProject & BuildableItem,
        W extends BuildWrapper> implements Action {

    private static final Logger log = Logger.getLogger(ReleaseAction.class.getName());
    public static final String RT_RELEASE_STAGING = "RT_RELEASE_STAGING_";

    protected final transient P project;
    protected VERSIONING versioning;
    /**
     * The next release version to change the model to if using one global version.
     */
    protected String releaseVersion;
    /**
     * Next (development) version to change the model to if using one global version.
     */
    protected String nextVersion;
    protected transient boolean strategyRequestFailed = false;
    protected transient String strategyRequestErrorMessage = null;
    protected transient boolean strategyPluginExists;
    protected transient Map stagingStrategy;
    protected transient String defaultVersioning;
    protected transient VersionedModule defaultGlobalModule;
    protected transient Map<String, VersionedModule> defaultModules;
    protected transient VcsConfig defaultVcsConfig;
    protected transient PromotionConfig defaultPromotionConfig;
    Boolean pro;
    boolean createVcsTag;
    String tagUrl;
    String tagComment;
    String nextDevelCommitComment;
    String stagingRepositoryKey;
    String stagingComment;
    boolean createReleaseBranch;
    String releaseBranch;
    private Class<W> wrapperClass;
    boolean overrideCredentials;
    String username;
    String password;

    public ReleaseAction(P project, Class<W> wrapperClass) {
        this.project = project;
        this.wrapperClass = wrapperClass;
    }

    /**
     * invoked from the jelly file
     *
     * @throws Exception Any exception
     */
    @SuppressWarnings("UnusedDeclaration")
    public void init() throws Exception {
        initBuilderSpecific();
        resetFields();
        if (!UserPluginInfo.NO_PLUGIN_KEY.equals(getSelectedStagingPluginName())) {
            PluginSettings selectedStagingPluginSettings = getSelectedStagingPlugin();
            try {
                stagingStrategy = getArtifactoryServer().getStagingStrategy(selectedStagingPluginSettings, Util.rawEncode(project.getName()), project);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to obtain staging strategy: " + e.getMessage(), e);
                strategyRequestFailed = true;
                strategyRequestErrorMessage = "Failed to obtain staging strategy '" +
                        selectedStagingPluginSettings.getPluginName() + "': " + e.getMessage() +
                        ".\nPlease review the log for further information.";
                stagingStrategy = null;
            }
            strategyPluginExists = (stagingStrategy != null) && !stagingStrategy.isEmpty();
        }

        prepareDefaultVersioning();
        prepareDefaultGlobalModule();
        prepareDefaultModules();
        prepareDefaultVcsSettings();
        prepareDefaultPromotionConfig();
    }

    private void resetFields() {
        strategyRequestFailed = false;
        strategyRequestErrorMessage = null;
        strategyPluginExists = false;
        stagingStrategy = null;
        defaultVersioning = null;
        defaultGlobalModule = null;
        defaultModules = null;
        defaultVcsConfig = null;
        defaultPromotionConfig = null;
    }

    public boolean isStrategyRequestFailed() {
        return strategyRequestFailed;
    }

    public String getStrategyRequestErrorMessage() {
        return strategyRequestErrorMessage;
    }

    public abstract String getTargetRemoteName();

    public String getTitle() {
        StringBuilder titleBuilder = new StringBuilder("Artifactory Pro Release Staging");
        String pluginName = getSelectedStagingPluginName();
        if (strategyPluginExists && StringUtils.isNotBlank(pluginName)) {
            titleBuilder.append(" - Using the '").append(pluginName)
                    .append("' staging plugin.");
        }
        return titleBuilder.toString();
    }

    /**
     * @return The message to display on the left panel for the perform release action.
     */
    public String getDisplayName() {
        return "Artifactory Release Staging";
    }

    public String getUrlName() {
        return "release";
    }

    public VERSIONING getVersioning() {
        return versioning;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getNextVersion() {
        return nextVersion;
    }

    public boolean isCreateVcsTag() {
        return createVcsTag;
    }

    public String getTagUrl() {
        return tagUrl;
    }

    public String getTagComment() {
        return StringUtils.isNotBlank(tagComment) ? tagComment : getDefaultVcsConfig().getTagComment();
    }

    public boolean isOverrideCredentials() {
        return overrideCredentials;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNextDevelCommitComment() {
        return nextDevelCommitComment;
    }

    public boolean isCreateReleaseBranch() {
        return createReleaseBranch;
    }

    public String getReleaseBranch() {
        return releaseBranch;
    }

    public String getStagingComment() {
        return stagingComment;
    }

    public String getStagingRepositoryKey() {
        return stagingRepositoryKey;
    }

    public String latestVersioningSelection() {
        return VERSIONING.GLOBAL.name();
    }

    public String getDefaultGlobalReleaseVersion() {
        return (defaultGlobalModule != null) ? defaultGlobalModule.getReleaseVersion() : null;
    }

    public String getDefaultGlobalNextDevelopmentVersion() {
        return (defaultGlobalModule != null) ? defaultGlobalModule.getNextDevelopmentVersion() : null;
    }

    public Collection<VersionedModule> getDefaultModules() {
        return defaultModules.values();
    }

    public boolean isGit() {
        return AbstractScmCoordinator.isGitScm(project);
    }

    public VcsConfig getDefaultVcsConfig() {
        return defaultVcsConfig;
    }

    public PromotionConfig getDefaultPromotionConfig() {
        return defaultPromotionConfig;
    }

    /**
     * @return List of target repositories for deployment (release repositories first). Called from the UI.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public abstract List<String> getRepositoryKeys() throws IOException;

    @SuppressWarnings("UnusedDeclaration")
    public abstract boolean isArtifactoryPro();

    public abstract ArtifactoryServer getArtifactoryServer();

    /**
     * This method is used to initiate a release staging process using the Artifactory Release Staging API.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void doApi(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        try {
            log.log(Level.INFO, "Initiating Artifactory Release Staging using API");
            // Enforce release permissions
            project.checkPermission(ArtifactoryPlugin.RELEASE);
            // In case a staging user plugin is configured, the init() method invoke it:
            init();
            // Read the values provided by the staging user plugin and assign them to data members in this class.
            // Those values can be overriden by URL arguments sent with the API:
            readStagingPluginValues();
            // Read values from the request and override the staging plugin values:
            overrideStagingPluginParams(req);
            // Schedule the release build:
            Queue.WaitingItem item = Jenkins.get().getQueue().schedule(
                project, 0,
                new Action[]{this, new CauseAction(new Cause.UserIdCause())}
            );
            if (item == null) {
                log.log(Level.SEVERE, "Failed to schedule a release build following a Release API invocation");
                resp.setStatus(StaplerResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                String url = req.getContextPath() + '/' + item.getUrl();
                JSONObject json = new JSONObject();
                json.element("queueItem", item.getId());
                json.element("releaseVersion", getReleaseVersion());
                json.element("nextVersion", getNextVersion());
                json.element("releaseBranch", getReleaseBranch());
                // Must use getOutputStream as sendRedirect uses getOutputStream (and closes it)
                resp.getOutputStream().print(json.toString());
                resp.sendRedirect(201, url);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Artifactory Release Staging API invocation failed: " + e.getMessage(), e);
            resp.setStatus(StaplerResponse.SC_INTERNAL_SERVER_ERROR);
            ErrorResponse errorResponse = new ErrorResponse(StaplerResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            resp.getWriter().write(mapper.writeValueAsString(errorResponse));
        }
    }

    /**
     * Form submission is calling this method
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @RequirePOST
    public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        // Enforce release permissions
        project.checkPermission(ArtifactoryPlugin.RELEASE);
        readRequestParams(req, false);
        // Schedule release build
        if (project.scheduleBuild(0, new Cause.UserIdCause(), this)) {
            // Redirect to the project page
            resp.sendRedirect(project.getAbsoluteUrl());
        }
    }

    private void overrideStagingPluginParams(StaplerRequest req) throws Exception {
        req.bindParameters(this);
        String versioningStr = req.getParameter("versioning");
        if (versioningStr != null) {
            versioning = VERSIONING.valueOf(versioningStr);
            switch (versioning) {
                case GLOBAL:
                    doGlobalVersioning(req);
                    break;
                case PER_MODULE:
                    doPerModuleVersioning(req);
            }
        }

        if (req.getParameter("createVcsTag") != null) {
            createVcsTag = Boolean.valueOf(req.getParameter("createVcsTag"));
        }
        if (req.getParameter("tagUrl") != null) {
            tagUrl = req.getParameter("tagUrl");
        }
        if (req.getParameter("tagComment") != null) {
            tagComment = req.getParameter("tagComment");
        }
        if (req.getParameter("createReleaseBranch") != null) {
            createReleaseBranch = Boolean.valueOf(req.getParameter("createReleaseBranch"));
        }
        if (req.getParameter("releaseBranch") != null) {
            releaseBranch = req.getParameter("releaseBranch");
        }
        if (req.getParameter("nextDevelCommitComment") != null) {
            nextDevelCommitComment = req.getParameter("nextDevelCommitComment");
        }
        if (req.getParameter("repositoryKey") != null) {
            stagingRepositoryKey = req.getParameter("repositoryKey");
        }
        if (req.getParameter("stagingComment") != null) {
            stagingComment = req.getParameter("stagingComment");
        }
        if (req.getParameter("overrideCredentials") != null) {
            overrideCredentials = Boolean.valueOf(req.getParameter("overrideCredentials"));
        }
        if (req.getParameter("username") != null) {
            username = req.getParameter("username");
        }
        if (req.getParameter("password") != null) {
            password = req.getParameter("password");
        }


    }

    private void readRequestParams(StaplerRequest req, boolean api) {
        req.bindParameters(this);

        String versioningStr = req.getParameter("versioning");
        versioning = VERSIONING.valueOf(versioningStr);
        switch (versioning) {
            case GLOBAL:
                doGlobalVersioning(req);
                break;
            case PER_MODULE:
                doPerModuleVersioning(req);
        }

        createVcsTag = req.getParameter("createVcsTag") != null;
        if (createVcsTag) {
            tagUrl = req.getParameter("tagUrl");
            tagComment = req.getParameter("tagComment");
        }

        overrideCredentials = req.getParameter("overrideCredentials") != null;
        if (overrideCredentials) {
            username = req.getParameter("username");
            password = req.getParameter("password");
        }

        nextDevelCommitComment = req.getParameter("nextDevelCommitComment");
        createReleaseBranch = req.getParameter("createReleaseBranch") != null;
        if (createReleaseBranch) {
            releaseBranch = req.getParameter("releaseBranch");
        }

        stagingRepositoryKey = req.getParameter("repositoryKey");
        stagingComment = req.getParameter("stagingComment");
    }

    public abstract String getReleaseVersionFor(Object moduleName);

    public abstract String getNextVersionFor(Object moduleName);

    protected void initBuilderSpecific() throws Exception {
    }

    protected String getDefaultNextDevelCommitMessage() {
        return SubversionManager.COMMENT_PREFIX + "Next development version";
    }

    protected String getBaseTagUrlAccordingToScm(String baseTagUrl) {
        if (AbstractScmCoordinator.isSvn(project) && !baseTagUrl.endsWith("/")) {
            return baseTagUrl + "/";
        }
        return baseTagUrl;
    }

    /**
     * Execute the {@link VERSIONING#GLOBAL} strategy of the versioning mechanism, which assigns all modules the same
     * version for the release and for the next development version.
     *
     * @param req The request that is coming from the form when staging.
     */
    protected void doGlobalVersioning(StaplerRequest req) {
        if (req.getParameter("releaseVersion") != null) {
            releaseVersion = req.getParameter("releaseVersion");
        }
        if (req.getParameter("nextVersion") != null) {
            nextVersion = req.getParameter("nextVersion");
        }
    }

    protected void doGlobalVersioning() {
        if (defaultGlobalModule != null) {
            releaseVersion = defaultGlobalModule.getReleaseVersion();
            nextVersion = defaultGlobalModule.getNextDevelopmentVersion();
        }
    }

    protected W getWrapper() {
        return ActionableHelper.getBuildWrapper(project, wrapperClass);
    }

    protected String calculateReleaseVersion(String fromVersion) {
        return fromVersion.replace("-SNAPSHOT", "");
    }

    /**
     * Calculates the next snapshot version based on the current release version
     *
     * @param fromVersion The version to bump to next development version
     * @return The next calculated development (snapshot) version
     */
    protected String calculateNextVersion(String fromVersion) {
        // first turn it to release version
        fromVersion = calculateReleaseVersion(fromVersion);
        String nextVersion;
        int lastDotIndex = fromVersion.lastIndexOf('.');
        try {
            if (lastDotIndex != -1) {
                // probably a major minor version e.g., 2.1.1
                String minorVersionToken = fromVersion.substring(lastDotIndex + 1);
                String nextMinorVersion;
                int lastDashIndex = minorVersionToken.lastIndexOf('-');
                if (lastDashIndex != -1) {
                    // probably a minor-buildNum e.g., 2.1.1-4 (should change to 2.1.1-5)
                    String buildNumber = minorVersionToken.substring(lastDashIndex + 1);
                    int nextBuildNumber = Integer.parseInt(buildNumber) + 1;
                    nextMinorVersion = minorVersionToken.substring(0, lastDashIndex + 1) + nextBuildNumber;
                } else {
                    nextMinorVersion = Integer.parseInt(minorVersionToken) + 1 + "";
                }
                nextVersion = fromVersion.substring(0, lastDotIndex + 1) + nextMinorVersion;
            } else {
                // maybe it's just a major version; try to parse as an int
                int nextMajorVersion = Integer.parseInt(fromVersion) + 1;
                nextVersion = nextMajorVersion + "";
            }
        } catch (NumberFormatException e) {
            return fromVersion;
        }
        return nextVersion + "-SNAPSHOT";
    }

    /**
     * Execute the {@link VERSIONING#PER_MODULE} strategy of the versioning mechanism, which assigns each module its own
     * version for release and for the next development version
     *
     * @param req The request that is coming from the form when staging.
     */
    protected abstract void doPerModuleVersioning(StaplerRequest req);

    protected abstract void doPerModuleVersioning(Map<String, VersionedModule> defaultModules);

    protected abstract PluginSettings getSelectedStagingPlugin() throws Exception;

    protected abstract String getSelectedStagingPluginName();

    protected abstract void prepareBuilderSpecificDefaultGlobalModule();

    protected abstract void prepareBuilderSpecificDefaultModules();

    protected abstract void prepareBuilderSpecificDefaultVcsConfig();

    protected abstract void prepareBuilderSpecificDefaultPromotionConfig() throws IOException;

    protected void prepareBuilderSpecificDefaultVersioning() {
    }

    /**
     * Read the values provided by the staging user plugin and assign them to data members in this class.
     * Those values can be overriden by URL arguments sent with the API:
     */
    private void readStagingPluginValues() {
        versioning = VERSIONING.valueOf(defaultVersioning);

        createVcsTag = defaultVcsConfig.isCreateTag();
        overrideCredentials = defaultVcsConfig.isOverrideCredentials();
        username = defaultVcsConfig.getUsername();
        password = defaultVcsConfig.getPassword();
        tagUrl = defaultVcsConfig.getTagUrlOrName();
        tagComment = defaultVcsConfig.getTagComment();
        nextDevelCommitComment = defaultVcsConfig.getNextDevelopmentVersionComment();
        createReleaseBranch = defaultVcsConfig.isUseReleaseBranch();
        releaseBranch = defaultVcsConfig.getReleaseBranchName();
        stagingRepositoryKey = defaultPromotionConfig.getTargetRepository();
        stagingComment = defaultPromotionConfig.getComment();

        switch (versioning) {
            case GLOBAL:
                doGlobalVersioning();
                break;
            case PER_MODULE:
                doPerModuleVersioning(defaultModules);
        }
    }

    private void prepareDefaultVersioning() {
        if (strategyPluginExists) {
            if (stagingStrategy.containsKey("defaultModuleVersion")) {
                defaultVersioning = VERSIONING.GLOBAL.name();
            } else if (stagingStrategy.containsKey("moduleVersionsMap")) {
                defaultVersioning = VERSIONING.PER_MODULE.name();
            }
        }

        if (StringUtils.isBlank(defaultVersioning)) {
            prepareBuilderSpecificDefaultVersioning();
        }
    }

    private void prepareDefaultGlobalModule() {
        if (strategyPluginExists) {
            if (stagingStrategy.containsKey("defaultModuleVersion")) {
                Map<String, String> defaultModuleVersion =
                        (Map<String, String>) stagingStrategy.get("defaultModuleVersion");
                defaultGlobalModule = new VersionedModule(defaultModuleVersion.get("moduleId"),
                        defaultModuleVersion.get("nextRelease"), defaultModuleVersion.get("nextDevelopment"));
            }
        }

        if (defaultGlobalModule == null) {
            prepareBuilderSpecificDefaultGlobalModule();
        }
    }

    private void prepareDefaultModules() {
        if (strategyPluginExists) {
            if (stagingStrategy.containsKey("moduleVersionsMap")) {
                Map<String, ? extends Map<String, String>> moduleVersionsMap =
                        (Map<String, ? extends Map<String, String>>) stagingStrategy.get("moduleVersionsMap");
                defaultModules = Maps.newHashMap();
                if (!moduleVersionsMap.isEmpty()) {
                    for (Map<String, String> moduleVersion : moduleVersionsMap.values()) {
                        String moduleId = moduleVersion.get("moduleId");
                        defaultModules.put(moduleId, new VersionedModule(moduleId, moduleVersion.get("nextRelease"),
                                moduleVersion.get("nextDevelopment")));
                    }
                }
            }
        }

        if (defaultModules == null) {
            prepareBuilderSpecificDefaultModules();
        }
    }

    private void prepareDefaultVcsSettings() {
        if (strategyPluginExists) {
            if (stagingStrategy.containsKey("vcsConfig")) {
                Map<String, Object> vcsConfig = (Map<String, Object>) stagingStrategy.get("vcsConfig");
                defaultVcsConfig = new VcsConfig(((Boolean) vcsConfig.get("useReleaseBranch")),
                        getStagingConfigAsString(vcsConfig, "releaseBranchName"),
                        ((Boolean) vcsConfig.get("createTag")),
                        getStagingConfigAsString(vcsConfig, "tagUrlOrName"),
                        getStagingConfigAsString(vcsConfig, "tagComment"),
                        getStagingConfigAsString(vcsConfig, "nextDevelopmentVersionComment"));
            }
        }

        if (defaultVcsConfig == null) {
            prepareBuilderSpecificDefaultVcsConfig();
        }
    }

    private void prepareDefaultPromotionConfig() throws IOException {
        if (strategyPluginExists) {
            if (stagingStrategy.containsKey("promotionConfig")) {
                Map<String, String> promotionConfig = (Map<String, String>) stagingStrategy.get("promotionConfig");
                defaultPromotionConfig = new PromotionConfig(promotionConfig.get("targetRepository"),
                        promotionConfig.get("comment"));
            }
        }

        if (defaultPromotionConfig == null) {
            prepareBuilderSpecificDefaultPromotionConfig();
        }
    }

    private String getStagingConfigAsString(Map<String, Object> configMap, String key) {
        if (configMap.containsKey(key)) {
            return configMap.get(key).toString();
        }
        return null;
    }

    protected List<hudson.tasks.Builder> getBuilders() {
        if (project instanceof MatrixProject)
            return ((MatrixProject) project).getBuilders();
        if (project instanceof MatrixConfiguration)
            return ((MatrixConfiguration) project).getBuilders();

        return ((FreeStyleProject) project).getBuilders();
    }

    public StandardCredentials getGitCredentials() {
        if (overrideCredentials) {
            return new UsernamePasswordCredentialsImpl(null, null, "release staging Git credentials", username, password);
        }
        return null;
    }

    public enum VERSIONING {
        GLOBAL("One version for all modules"),
        PER_MODULE("Version per module"),
        NONE("Use existing module versions");
        // The description to display in the UI
        private final String displayMessage;

        VERSIONING(String displayMessage) {
            this.displayMessage = displayMessage;
        }

        public String getDisplayMessage() {
            return displayMessage;
        }
    }

    /**
     * Add some of the release build properties to a map.
     */
    public void addVars(Map<String, String> env) {
        if (tagUrl != null) {
            env.put("RELEASE_SCM_TAG", tagUrl);
            env.put(RT_RELEASE_STAGING + "SCM_TAG", tagUrl);
        }
        if (releaseBranch != null) {
            env.put("RELEASE_SCM_BRANCH", releaseBranch);
            env.put(RT_RELEASE_STAGING + "SCM_BRANCH", releaseBranch);
        }
        if (releaseVersion != null) {
            env.put(RT_RELEASE_STAGING + "VERSION", releaseVersion);
        }
        if (nextVersion != null) {
            env.put(RT_RELEASE_STAGING + "NEXT_VERSION", nextVersion);
        }
    }
}
