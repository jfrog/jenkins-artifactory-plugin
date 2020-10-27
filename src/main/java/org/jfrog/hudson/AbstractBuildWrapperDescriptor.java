package org.jfrog.hudson;

import com.google.common.collect.Maps;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.RefreshServerResponse;
import org.jfrog.hudson.util.RepositoriesUtils;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author yahavi
 */
public abstract class AbstractBuildWrapperDescriptor extends BuildWrapperDescriptor {
    protected AbstractProject<?, ?> item;
    private String configPrefix;
    private String displayName;

    public AbstractBuildWrapperDescriptor(Class<? extends BuildWrapper> configuratorClass, String displayName, String configPrefix) {
        super(configuratorClass);
        this.configPrefix = configPrefix;
        this.displayName = displayName;
        load();
    }

    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
        this.item = item;
        Class<?> itemClass = item.getClass();
        return itemClass.isAssignableFrom(FreeStyleProject.class) || itemClass.isAssignableFrom(MatrixProject.class) ||
                (Jenkins.get().getPlugin(PluginsUtils.MULTIJOB_PLUGIN_ID) != null &&
                        itemClass.isAssignableFrom(MultiJobProject.class)) ||
                PluginsUtils.PROMOTION_BUILD_PLUGIN_CLASS.equals(itemClass.getSimpleName());
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        req.bindParameters(this, configPrefix);
        save();
        return true;
    }

    /**
     * Returns the list of {@link org.jfrog.hudson.ArtifactoryServer} configured.
     *
     * @return can be empty but never null.
     */
    public List<ArtifactoryServer> getArtifactoryServers() {
        return RepositoriesUtils.getArtifactoryServers();
    }

    @SuppressWarnings("unused")
    public boolean isUseCredentialsPlugin() {
        return PluginsUtils.isUseCredentialsPlugin();
    }

    @SuppressWarnings("unused")
    public boolean isJiraPluginEnabled() {
        return (Jenkins.get().getPlugin("jira") != null);

    }

    /**
     * This method is triggered from the client side by ajax call.
     * The method is triggered by the "Refresh Repositories" button.
     *
     * @param url           Artifactory url
     * @param credentialsId credentials Id if using Credentials plugin
     * @param username      credentials legacy mode username
     * @param password      credentials legacy mode password
     * @return {@link org.jfrog.hudson.util.RefreshServerResponse} object that represents the response of the repositories
     */
    @SuppressWarnings("unused")
    protected RefreshServerResponse refreshResolversFromArtifactory(String url, String credentialsId, String username,
                                                                    String password, boolean overrideCredentials) {
        RefreshServerResponse response = new RefreshServerResponse();
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId, overrideCredentials);
        ArtifactoryServer artifactoryServer = RepositoriesUtils.getArtifactoryServer(url, RepositoriesUtils.getArtifactoryServers());
        try {
            List<VirtualRepository> virtualRepositories = refreshVirtualRepositories(artifactoryServer, credentialsConfig);
            response.setVirtualRepositories(virtualRepositories);
            response.setSuccess(true);
        } catch (Exception e) {
            response.setResponseMessage(e.getMessage());
            response.setSuccess(false);
        }

        return response;
    }

    /**
     * This method triggered from the client side by Ajax call.
     * The Element that trig is the "Refresh Repositories" button.
     *
     * @param url                 Artifactory url
     * @param credentialsId       credentials Id if using Credentials plugin
     * @param username            credentials legacy mode username
     * @param password            credentials legacy mode password
     * @param overrideCredentials credentials legacy mode overridden
     * @param refreshUserPlugins  true if user plugin lists should be retrieved
     * @return {@link org.jfrog.hudson.util.RefreshServerResponse} object that represents the response of the repositories
     */
    protected RefreshServerResponse refreshDeployersFromArtifactory(String url, String credentialsId, String username,
                                                                    String password, boolean overrideCredentials, boolean refreshUserPlugins) {
        RefreshServerResponse response = new RefreshServerResponse();
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId, overrideCredentials);
        ArtifactoryServer artifactoryServer = RepositoriesUtils.getArtifactoryServer(url, RepositoriesUtils.getArtifactoryServers());
        try {
            response.setRepositories(refreshRepositories(artifactoryServer, credentialsConfig));
            if (refreshUserPlugins) {
                response.setUserPlugins(refreshUserPlugins(artifactoryServer, credentialsConfig));
            }
            response.setSuccess(true);
        } catch (Exception e) {
            response.setResponseMessage(e.getMessage());
            response.setSuccess(false);
        }
        return response;
    }

    private List<PluginSettings> refreshUserPlugins(ArtifactoryServer artifactoryServer, final CredentialsConfig credentialsConfigs) {
        List<UserPluginInfo> pluginInfoList = artifactoryServer.getStagingUserPluginInfo(new DeployerOverrider() {
            public boolean isOverridingDefaultDeployer() {
                return credentialsConfigs != null && credentialsConfigs.isCredentialsProvided();
            }

            public Credentials getOverridingDeployerCredentials() {
                return null;
            }

            public CredentialsConfig getDeployerCredentialsConfig() {
                return credentialsConfigs;
            }
        }, item);

        ArrayList<PluginSettings> list = new ArrayList<>(pluginInfoList.size());
        for (UserPluginInfo p : pluginInfoList) {
            Map<String, String> paramsMap = Maps.newHashMap();
            List<UserPluginInfoParam> params = p.getPluginParams();
            for (UserPluginInfoParam param : params) {
                paramsMap.put(((String) param.getKey()), ((String) param.getDefaultValue()));
            }

            PluginSettings plugin = new PluginSettings(p.getPluginName(), paramsMap);
            list.add(plugin);
        }

        return list;
    }

    private List<VirtualRepository> refreshVirtualRepositories(ArtifactoryServer artifactoryServer,
                                                               CredentialsConfig credentialsConfig) throws IOException {
        List<VirtualRepository> virtualRepositories = RepositoriesUtils.getVirtualRepositoryKeys(artifactoryServer.getArtifactoryUrl(),
                credentialsConfig, artifactoryServer, item);
        Collections.sort(virtualRepositories);
        return virtualRepositories;
    }

    private List<Repository> refreshRepositories(ArtifactoryServer artifactoryServer, CredentialsConfig credentialsConfig)
            throws IOException {
        List<String> releaseRepositoryKeysFirst = RepositoriesUtils.getLocalRepositories(artifactoryServer.getArtifactoryUrl(),
                credentialsConfig, artifactoryServer, item);
        Collections.sort(releaseRepositoryKeysFirst);
        return RepositoriesUtils.createRepositoriesList(releaseRepositoryKeysFirst);
    }

    public boolean isMultiConfProject() {
        return (item.getClass().isAssignableFrom(MatrixProject.class));
    }

}
