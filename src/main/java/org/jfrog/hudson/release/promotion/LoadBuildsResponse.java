package org.jfrog.hudson.release.promotion;

import org.jfrog.hudson.Repository;
import org.jfrog.hudson.UserPluginInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yahavi on 14/03/2017.
 */
public class LoadBuildsResponse {

    private List<Repository> repositories = new ArrayList<Repository>();
    private List<UserPluginInfo> plugins = new ArrayList<UserPluginInfo>();
    private List<PromotionConfig> promotionConfigs;
    private String responseMessage = "Build not found";
    private boolean success;

    @SuppressWarnings({"UnusedDeclaration"})
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

    public List<Repository> getRepositories() {
        return this.repositories;
    }

    public void addRepositories(List<String> repositories) {
        for (String repository : repositories) {
            this.repositories.add(new Repository(repository));
        }
    }

    public List<UserPluginInfo> getPlugins() {
        return this.plugins;
    }

    public void setPlugins(List<UserPluginInfo> plugins) {
        this.plugins = plugins;
    }

    public List<PromotionConfig> getPromotionConfigs() {
        return this.promotionConfigs;
    }

    public void setPromotionConfigs(List<PromotionConfig> promotionConfigs) {
        this.promotionConfigs = promotionConfigs;
    }
}