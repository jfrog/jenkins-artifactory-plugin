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

package org.jfrog.hudson.release;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.jfrog.build.api.builder.PromotionBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.hudson.*;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.CredentialManager;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This badge action is added to a successful staged builds. It allows performing additional promotion.
 *
 * @author Noam Y. Tenne
 */
public class UnifiedPromoteBuildAction<C extends BuildInfoAwareConfigurator & DeployerOverrider> extends TaskAction
        implements BuildBadgeAction {
    private final AbstractBuild build;
    private final C configurator;

    private String targetStatus;
    private String repositoryKey;
    private String sourceRepositoryKey;
    private String comment;
    private boolean useCopy;
    private boolean failFast = true;
    private boolean includeDependencies;
    private PluginSettings promotionPlugin;
    private String defaultPromotionRepositoryKey;

    public UnifiedPromoteBuildAction(AbstractBuild build, C configurator) {
        this.build = build;
        this.configurator = configurator;
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

    public AbstractBuild getBuild() {
        return build;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public void setRepositoryKey(String repositoryKey) {
        this.repositoryKey = repositoryKey;
    }

    public String getSourceRepositoryKey() {
        return sourceRepositoryKey;
    }

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

    public String getPromotionPluginName() {
        return (promotionPlugin != null) ? promotionPlugin.getPluginName() : null;
    }

    public void setPromotionPlugin(PluginSettings promotionPlugin) {
        this.promotionPlugin = promotionPlugin;
    }

    public String getPluginParamValue(String pluginName, String paramKey) {
        return (promotionPlugin != null) ? promotionPlugin.getPluginParamValue(pluginName, paramKey) : null;
    }

    /**
     * @return List of target repositories for deployment (release repositories first). Called from the UI.
     */
    public List<String> getRepositoryKeys() {
        ArtifactoryServer artifactoryServer = configurator.getArtifactoryServer();
        if (artifactoryServer == null) {
            return Lists.newArrayList();
        }
        List<String> repos = artifactoryServer.getReleaseRepositoryKeysFirst(configurator, build.getProject());
        repos.add(0, "");  // option not to move
        return repos;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public List<String> getTargetStatuses() {
        return Lists.newArrayList(/*"Staged", */"Released", "Rolled-back");
    }

    public String getDefaultPromotionTargetRepository() {
        if (repositoryKey == null) {
            return configurator.getDefaultPromotionTargetRepository();
        }
        return repositoryKey;
    }

    /**
     * @return The repository selected by the latest promotion (to be selected by default).
     */
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
    public void doSubmit(StaplerRequest req, StaplerResponse resp) throws IOException, ServletException {
        getACL().checkPermission(getPermission());

        bindParameters(req);
        // current user is bound to the thread and will be lost in the perform method
        User user = User.current();
        String ciUser = (user == null) ? "anonymous" : user.getId();

        JSONObject formData = req.getSubmittedForm();
        if (formData.has("promotionPlugin")) {
            JSONObject pluginSettings = formData.getJSONObject("promotionPlugin");
            if (pluginSettings.has("pluginName")) {
                String pluginName = pluginSettings.getString("pluginName");
                if (!UserPluginInfo.NO_PLUGIN_KEY.equals(pluginName)) {
                    PluginSettings settings = new PluginSettings();
                    Map<String, String> paramMap = Maps.newHashMap();
                    settings.setPluginName(pluginName);
                    Map<String, Object> filteredPluginSettings = Maps.filterKeys(pluginSettings,
                            new Predicate<String>() {
                                public boolean apply(String input) {
                                    return StringUtils.isNotBlank(input) && !"pluginName".equals(input);
                                }
                            });
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

        ArtifactoryServer server = configurator.getArtifactoryServer();

        new PromoteWorkerThread(server, CredentialManager.getPreferredDeployer(configurator, server), ciUser).start();

        resp.sendRedirect(".");
    }

    private void bindParameters(StaplerRequest req) throws ServletException {
        req.bindParameters(this);
        JSONObject formData = req.getSubmittedForm();
        JSONObject pluginSettings = formData.getJSONObject("promotionPlugin");

        // StaplerRequest.bindParameters doesn't work well with jelly <f:checkbox> element,
        // so we set the "boolean" fields manually
        if (pluginSettings.get("includeDependencies") != null) {
            this.setIncludeDependencies(pluginSettings.getBoolean("includeDependencies"));
        }
        if (pluginSettings.get("useCopy") != null) {
            this.setUseCopy(pluginSettings.getBoolean("useCopy"));
        }

        if (pluginSettings.get("failFast") != null) {
            this.setFailFast(pluginSettings.getBoolean("failFast"));
        }
    }

    public List<UserPluginInfo> getPromotionsUserPluginInfo() {
        ArtifactoryServer artifactoryServer = configurator.getArtifactoryServer();
        if (artifactoryServer == null) {
            return Lists.newArrayList(UserPluginInfo.NO_PLUGIN);
        }
        return artifactoryServer.getPromotionsUserPluginInfo(configurator, build.getProject());
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

        private final ArtifactoryServer artifactoryServer;
        private final CredentialsConfig deployerConfig;
        private final String ciUser;

        public PromoteWorkerThread(ArtifactoryServer artifactoryServer, CredentialsConfig deployerConfig, String ciUser) {
            super(UnifiedPromoteBuildAction.this, ListenerAndText.forMemory(null));
            this.artifactoryServer = artifactoryServer;
            this.deployerConfig = deployerConfig;
            this.ciUser = ciUser;
        }

        @Override
        protected void perform(TaskListener listener) {
            ArtifactoryBuildInfoClient client = null;
            try {
                long started = System.currentTimeMillis();
                listener.getLogger().println("Promoting build ....");

                client = artifactoryServer.createArtifactoryClient(deployerConfig.provideUsername(build.getProject()), deployerConfig.providePassword(build.getProject()),
                        artifactoryServer.createProxyConfiguration(Jenkins.getInstance().proxy));

                if ((promotionPlugin != null) &&
                        !UserPluginInfo.NO_PLUGIN_KEY.equals(promotionPlugin.getPluginName())) {
                    handlePluginPromotion(listener, client);
                } else {
                    PromotionBuilder promotionBuilder = new PromotionBuilder()
                            .status(targetStatus)
                            .comment(comment)
                            .ciUser(ciUser)
                            .targetRepo(repositoryKey)
                            .sourceRepo(sourceRepositoryKey)
                            .dependencies(includeDependencies)
                            .copy(useCopy)
                            .failFast(failFast);

                    String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(configurator, build);
                    String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
                    PromotionUtils.promoteAndCheckResponse(promotionBuilder, client, listener, buildName, buildNumber);
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
            } finally {
                if (client != null) {
                    client.shutdown();
                }
            }
        }

        private void handlePluginPromotion(TaskListener listener, ArtifactoryBuildInfoClient client)
                throws IOException {
            String buildName = BuildUniqueIdentifierHelper.getBuildNameConsiderOverride(configurator, build);
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            HttpResponse pluginPromotionResponse = client.executePromotionUserPlugin(
                    promotionPlugin.getPluginName(), buildName, buildNumber, promotionPlugin.getParamMap());
            if (PromotionUtils.checkSuccess(pluginPromotionResponse, false, failFast, false, listener)) {
                listener.getLogger().println("Promotion completed successfully!");
            }
        }
    }
}
