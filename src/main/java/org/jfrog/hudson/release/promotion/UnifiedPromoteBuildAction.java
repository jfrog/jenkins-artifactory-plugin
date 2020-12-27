/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.hudson.release.promotion;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.Permission;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.*;
import org.jfrog.hudson.release.PromotionUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.CredentialManager;
import org.jfrog.hudson.util.ProxyUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jfrog.hudson.util.SerializationUtils.createMapper;

/**
 * This badge action is added to a successful staged builds. It allows performing additional promotion.
 *
 * @author Noam Y. Tenne
 */
public class UnifiedPromoteBuildAction extends TaskAction implements BuildBadgeAction {
    private final Run<?, ?> build;
    private final Map<String, PromotionInfo> promotionCandidates = new HashMap<>();
    private String targetStatus;
    private String targetRepositoryKey;
    private String sourceRepositoryKey;
    private String comment;
    private boolean useCopy;
    private boolean failFast = true;
    private boolean includeDependencies;
    private PluginSettings promotionPlugin;

    public UnifiedPromoteBuildAction(Run<?, ?> build) {
        this.build = build;
    }

    public UnifiedPromoteBuildAction(Run<?, ?> build, BuildInfoAwareConfigurator configurator) {
        this(build);
        String buildName = BuildUniqueIdentifierHelper.
                getBuildNameConsiderOverride(configurator, build);
        String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
        PromotionConfig promotionConfig = new PromotionConfig();
        promotionConfig.setBuildName(buildName);
        promotionConfig.setBuildNumber(buildNumber);
        addPromotionCandidate(promotionConfig, configurator, null);
    }

    public void addPromotionCandidate(PromotionConfig promotionConfig, BuildInfoAwareConfigurator configurator, String displayName) {
        PromotionInfo promotionCandidate = new PromotionInfo(promotionConfig, configurator, promotionCandidates.size(), displayName);
        promotionCandidates.put(promotionCandidate.getId(), promotionCandidate);
    }

    private BuildInfoAwareConfigurator getFirstConfigurator() {
        return promotionCandidates.get("0").getConfigurator();
    }

    private PromotionInfo getPromotionCandidate(String id) {
        return promotionCandidates.get(id);
    }

    private String getDefaultPromotionTargetRepository() {
        BuildInfoAwareConfigurator configurator = getFirstConfigurator();
        return configurator != null ? configurator.getDefaultPromotionTargetRepository() : "";
    }

    /**
     * @return List of promote infos for deployment. Called from the UI.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public List<PromotionInfo> getPromotionCandidates() {
        return new ArrayList<>(promotionCandidates.values());
    }

    /**
     * Load the related repositories, plugins and a promotion config associated to all builds
     * relevant for this build promotion action.
     * Called from the UI.
     *
     * @return JSON string representation of the LoadBuildsResponse class.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public String getBuildsData() {
        try {
            return createMapper().writeValueAsString(loadBuilds());
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * Load the related repositories, plugins and a promotion config associated to the buildId.
     * Called from the UI.
     *
     * @return LoadBuildsResponse e.g. list of repositories, plugins and a promotion config.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private LoadBuildsResponse loadBuilds() {
        LoadBuildsResponse response = new LoadBuildsResponse();
        // When we load a new build we need also to reset the promotion plugin.
        // The null plugin is related to 'None' plugin.
        setPromotionPlugin(null);
        try {
            List<String> repositoryKeys = getRepositoryKeys();
            List<UserPluginInfo> plugins = getPromotionsUserPluginInfo();
            response.addRepositories(repositoryKeys);
            response.setPlugins(plugins);
            response.setPromotionConfigs(getPromotionConfigs(repositoryKeys));
            response.setSuccess(true);
        } catch (Exception e) {
            // Set error message to display in the promotion page.
            // The error message should not contain new lines and double quotes.
            String message = e.getMessage()
                    .replace("\n", " ")
                    .replace("\"", "");
            response.setResponseMessage(message);
        }
        return response;
    }

    @Override
    protected Permission getPermission() {
        return ArtifactoryPlugin.PROMOTE;
    }

    public String getIconFileName() {
        return "/plugin/artifactory/images/artifactory-promote.png";
    }

    public String getDisplayName() {
        return "Artifactory Release Promotion";
    }

    public String getUrlName() {
        if (hasPromotionPermission()) {
            return "promote";
        }
        // return null to hide this action
        return null;
    }

    public boolean hasPromotionPermission() {
        return getACL().hasPermission(getPermission());
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setTargetRepositoryKey(String targetRepositoryKey) {
        this.targetRepositoryKey = targetRepositoryKey;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getSourceRepositoryKey() {
        return sourceRepositoryKey;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setSourceRepositoryKey(String sourceRepositoryKey) {
        this.sourceRepositoryKey = sourceRepositoryKey;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setUseCopy(boolean useCopy) {
        this.useCopy = useCopy;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getPromotionPluginName() {
        return (promotionPlugin != null) ? promotionPlugin.getPluginName() : null;
    }

    public void setPromotionPlugin(PluginSettings promotionPlugin) {
        this.promotionPlugin = promotionPlugin;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public String getPluginParamValue(String pluginName, String paramKey) {
        return (promotionPlugin != null) ? promotionPlugin.getPluginParamValue(pluginName, paramKey) : null;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public List<String> getTargetStatuses() {
        return Lists.newArrayList(/*"Staged", */"Released", "Rolled-back");
    }

    /**
     * @return List of target repositories for deployment (release repositories first). Called from the UI.
     */
    public List<String> getRepositoryKeys() throws IOException {
        final BuildInfoAwareConfigurator configurator = getFirstConfigurator();
        if (configurator == null) {
            return Lists.newArrayList();
        }

        ArtifactoryServer artifactoryServer = configurator.getArtifactoryServer();
        if (artifactoryServer == null) {
            return Lists.newArrayList();
        }
        List<String> repos = artifactoryServer.
                getReleaseRepositoryKeysFirst((DeployerOverrider) configurator, build.getParent());
        repos.add(0, "");  // option not to move
        return repos;
    }

    public List<PromotionConfig> getPromotionConfigs(List<String> repoKeys) {
        String defaultTargetRepo = getDefaultPromotionTargetRepository();
        boolean setTargetRepo = StringUtils.isNotBlank(defaultTargetRepo) && repoKeys.contains(defaultTargetRepo);

        List<PromotionConfig> configs = new ArrayList<>();
        for (PromotionInfo info : getPromotionCandidates()) {
            PromotionConfig config = info.getPromotionConfig();
            config.setId(info.getId());
            if (setTargetRepo) {
                config.setTargetRepo(defaultTargetRepo);
            }
            configs.add(config);
        }
        return configs;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public int getPromoteInfoListSize() {
        return promotionCandidates.size();
    }

    /**
     * @return The repository selected by the latest promotion (to be selected by default).
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public String lastPromotionSourceRepository() {
        return sourceRepositoryKey;
    }

    /**
     * Select which view to display based on the state of the promotion. Will return the form if user selects to perform
     * promotion. Progress will be returned if the promotion is currently in progress.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void doIndex(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        req.getView(this, chooseAction()).forward(req, resp);
    }

    /**
     * Form submission is calling this method
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @RequirePOST
    public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        getACL().checkPermission(getPermission());
        req.bindParameters(this);
        // Current user is bound to the thread and will be lost in the perform method
        User user = User.current();
        String ciUser = (user == null) ? "anonymous" : user.getId();

        JSONObject formData = req.getSubmittedForm();
        if (formData.has("promotionPlugin")) {
            configurePromotionPlugin(formData, ciUser);
        }

        String configuratorId = (String) formData.getOrDefault("promotionCandidates", "0");
        PromotionInfo promotionCandidate = getPromotionCandidate(configuratorId);
        BuildInfoAwareConfigurator configurator = promotionCandidate.getConfigurator();
        ArtifactoryServer server = configurator.getArtifactoryServer();

        new PromoteWorkerThread(promotionCandidate, CredentialManager.getPreferredDeployer((DeployerOverrider) configurator, server), ciUser).start();

        resp.sendRedirect(".");
    }

    private void configurePromotionPlugin(JSONObject formData, String ciUser) {
        JSONObject pluginSettings = formData.getJSONObject("promotionPlugin");
        if (pluginSettings.get("includeDependencies") != null) {
            this.setIncludeDependencies(pluginSettings.getBoolean("includeDependencies"));
        }
        if (pluginSettings.get("useCopy") != null) {
            this.setUseCopy(pluginSettings.getBoolean("useCopy"));
        }
        if (pluginSettings.get("failFast") != null) {
            this.setFailFast(pluginSettings.getBoolean("failFast"));
        }
        if (pluginSettings.has("pluginName")) {
            String pluginName = pluginSettings.getString("pluginName");
            if (!UserPluginInfo.NO_PLUGIN_KEY.equals(pluginName)) {
                PluginSettings settings = new PluginSettings();
                Map<String, String> paramMap = Maps.newHashMap();
                settings.setPluginName(pluginName);
                Map<String, Object> filteredPluginSettings = Maps.filterKeys(pluginSettings,
                        input -> StringUtils.isNotBlank(input) && !"pluginName".equals(input));
                for (Map.Entry<String, Object> settingsEntry : filteredPluginSettings.entrySet()) {
                    String key = settingsEntry.getKey();
                    paramMap.put(key, pluginSettings.getString(key));
                }
                paramMap.put("ciUser", ciUser);
                if (!paramMap.isEmpty()) {
                    settings.setParamMap(paramMap);
                }
                setPromotionPlugin(settings);
            }
        }
    }

    private List<UserPluginInfo> getPromotionsUserPluginInfo() {
        final BuildInfoAwareConfigurator configurator = getFirstConfigurator();
        if (configurator == null) {
            return Lists.newArrayList(UserPluginInfo.NO_PLUGIN);
        }

        ArtifactoryServer artifactoryServer = configurator.getArtifactoryServer();
        if (artifactoryServer == null) {
            return Lists.newArrayList(UserPluginInfo.NO_PLUGIN);
        }
        return artifactoryServer.getPromotionsUserPluginInfo((DeployerOverrider) configurator, build.getParent());
    }

    @Override
    protected ACL getACL() {
        return build.getACL();
    }

    private synchronized String chooseAction() {
        return workerThread == null ? "form.jelly" : "progress.jelly";
    }

    /**
     * The thread that performs the promotion asynchronously.
     */
    public final class PromoteWorkerThread extends TaskThread {

        private final PromotionInfo promotionCandidate;
        private final CredentialsConfig deployerConfig;
        private final String ciUser;

        PromoteWorkerThread(PromotionInfo promotionCandidate, CredentialsConfig deployerConfig, String ciUser) {
            super(UnifiedPromoteBuildAction.this, ListenerAndText.forMemory(null));
            this.promotionCandidate = promotionCandidate;
            this.deployerConfig = deployerConfig;
            this.ciUser = ciUser;
        }

        @Override
        protected void perform(TaskListener listener) {
            long started = System.currentTimeMillis();
            listener.getLogger().println("Promoting build ....");
            ArtifactoryServer server = promotionCandidate.getConfigurator().getArtifactoryServer();
            String buildName = promotionCandidate.getBuildName();
            String buildNumber = promotionCandidate.getBuildNumber();
            try (ArtifactoryBuildInfoClient client = server.createArtifactoryClient(deployerConfig.provideCredentials(build.getParent()),
                    ProxyUtils.createProxyConfiguration())) {
                if ((promotionPlugin != null) && !UserPluginInfo.NO_PLUGIN_KEY.equals(promotionPlugin.getPluginName())) {
                    handlePluginPromotion(listener, client, buildName, buildNumber);
                } else {
                    PromotionBuilder promotionBuilder = new PromotionBuilder()
                            .status(targetStatus)
                            .comment(comment)
                            .ciUser(ciUser)
                            .targetRepo(targetRepositoryKey)
                            .sourceRepo(sourceRepositoryKey)
                            .dependencies(includeDependencies)
                            .copy(useCopy)
                            .failFast(failFast);

                    PromotionUtils.promoteAndCheckResponse(promotionBuilder.build(), client, listener, buildName, buildNumber);
                }

                build.save();
                // if the client gets back to the progress (after the redirect) page when this thread already done,
                // she will get an error message because the log dies with the thread. So lets delay up to 2 seconds
                long timeToWait = 2000 - (System.currentTimeMillis() - started);
                if (timeToWait > 0) {
                    Thread.sleep(timeToWait);
                }
                workerThread = null;
            } catch (Throwable e) {
                e.printStackTrace(listener.error(e.getMessage()));
            }
        }

        private void handlePluginPromotion(TaskListener listener, ArtifactoryBuildInfoClient client, String buildName, String buildNumber) throws IOException {
            HttpEntity entity = null;
            try (CloseableHttpResponse response = client.executePromotionUserPlugin(promotionPlugin.getPluginName(), buildName, buildNumber, promotionPlugin.getParamMap())) {
                entity = response.getEntity();
                PromotionUtils.validatePromotionSuccessful(response, false, failFast, listener);
                listener.getLogger().println("Promotion completed successfully!");
            } catch (IOException e) {
                listener.error(e.getMessage());
            } finally {
                EntityUtils.consumeQuietly(entity);
            }
        }
    }
}