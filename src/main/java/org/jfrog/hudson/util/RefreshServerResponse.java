package org.jfrog.hudson.util;

import org.jfrog.hudson.PluginSettings;
import org.jfrog.hudson.Repository;
import org.jfrog.hudson.ResolutionRepository;

import java.util.List;

/**
 * Created by user on 25/06/2014.
 */
public class RefreshServerResponse {
    private List<Repository> repositories;
    private List<ResolutionRepository> resolutionRepositories;
    private List<PluginSettings> userPlugins;
    private String responseMessage;
    private boolean success;

    public List<Repository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<Repository> repositories) {
        this.repositories = repositories;
    }

    public List<ResolutionRepository> getResolutionRepositories() {
        return resolutionRepositories;
    }

    public void setResolutionRepositories(List<ResolutionRepository> resolutionRepositories) {
        this.resolutionRepositories = resolutionRepositories;
    }

    public List<PluginSettings> getUserPlugins() {
        return userPlugins;
    }

    public void setUserPlugins(List<PluginSettings> userPlugins) {
        this.userPlugins = userPlugins;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
