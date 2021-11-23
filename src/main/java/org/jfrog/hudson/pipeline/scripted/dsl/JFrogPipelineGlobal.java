package org.jfrog.hudson.pipeline.scripted.dsl;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JFrogPipelineGlobal implements Serializable {
    private static final String CREDENTIALS_ID = "credentialsId";
    private static final String DS_URL = "distributionUrl";
    private static final String RT_URL = "artifactoryUrl";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private static final String URL = "url";

    private final CpsScript cpsScript;

    public JFrogPipelineGlobal(CpsScript script) {
        this.cpsScript = script;
    }

    @Whitelisted
    public JFrogPlatformInstance instance(String instanceId) {
        Map<String, Object> stepVariables = new LinkedHashMap<>();
        stepVariables.put("instanceId", instanceId);
        JFrogPlatformInstance instance = (JFrogPlatformInstance) cpsScript.invokeMethod("getJFrogPlatformInstance", stepVariables);
        instance.setCpsScript(cpsScript);
        return instance;
    }

    @Whitelisted
    public JFrogPlatformInstance newInstance(Map<String, Object> stepVariables) {
        JFrogPlatformInstance instance = (JFrogPlatformInstance) cpsScript.invokeMethod("newJFrogPlatformInstance", stepVariables);
        instance.setCpsScript(cpsScript);
        return instance;
    }

    @Whitelisted
    public JFrogPlatformInstance newInstance(String url, String artifactoryUrl, String distributionUrl, String username, String password) {
        return newInstance(createInstanceParams(url, artifactoryUrl, distributionUrl, username, password, ""));
    }

    @Whitelisted
    public JFrogPlatformInstance newInstance(String url, String artifactoryUrl, String distributionUrl, String credentialsId) {
        return newInstance(createInstanceParams(url, artifactoryUrl, distributionUrl, "", "", credentialsId));
    }

    private Map<String, Object> createInstanceParams(String url, String artifactoryUrl, String distributionUrl, String username, String password, String credentialsId) {
        return new HashMap<String, Object>() {{
            put(URL, url);
            put(RT_URL, artifactoryUrl);
            put(DS_URL, distributionUrl);
            put(USERNAME, username);
            put(PASSWORD, password);
            put(CREDENTIALS_ID, credentialsId);
        }};
    }
}
