package org.jfrog.hudson.pipeline.types;

import com.google.common.collect.ArrayListMultimap;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by romang on 7/28/16.
 */
public class Docker implements Serializable {
    private transient CpsScript script;
    private String username;
    private String password;
    private String credentialsId;
    private String host;
    // Properties to attach to the deployed docker layers.
    private ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    private ArtifactoryServer server;

    public Docker() {
    }

    public Docker(CpsScript script, String username, String password, String credentialsId, String host) {
        this.script = script;
        this.username = username;
        this.password = password;
        this.credentialsId = credentialsId;
        this.host = host;
    }

    public void setCpsScript(CpsScript script) {
        this.script = script;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setServer(ArtifactoryServer server) {
        this.server = server;
    }

    @Whitelisted
    public Docker addProperty(String key, String... values) {
        properties.putAll(key, Arrays.asList(values));
        return this;
    }

    @Whitelisted
    public BuildInfo push(String imageTag, String targetRepository) throws Exception {
        return push(imageTag, targetRepository, null);
    }

    @Whitelisted
    public BuildInfo push(String imageTag, String targetRepository, BuildInfo providedBuildInfo) throws Exception {
        Map<String, Object> dockerArguments = new LinkedHashMap<String, Object>();
        dockerArguments.put("image", imageTag);
        dockerArguments.put("targetRepo", targetRepository);
        dockerArguments.put("buildInfo", providedBuildInfo);
        return push(dockerArguments);
    }

    @Whitelisted
    public BuildInfo push(Map<String, Object> dockerArguments) throws Exception {
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId);
        dockerArguments.put("credentialsConfig", credentialsConfig);
        dockerArguments.put("host", host);
        dockerArguments.put("properties", properties);
        dockerArguments.put("server", server);

        BuildInfo buildInfo;
        if (server != null) {
            buildInfo = (BuildInfo) script.invokeMethod("dockerPushStep", dockerArguments);
        } else {
            // Deprecated docker push step using proxy
            buildInfo = (BuildInfo) script.invokeMethod("dockerPushWithProxyStep", dockerArguments);
        }
        buildInfo.setCpsScript(script);
        return buildInfo;
    }

    @Whitelisted
    public BuildInfo pull(String imageTag) throws Exception {
        return pull(imageTag, null);
    }

    @Whitelisted
    public BuildInfo pull(String imageTag, BuildInfo providedBuildInfo) throws Exception {
        Map<String, Object> pullVariables = new LinkedHashMap<String, Object>();
        pullVariables.put("image", imageTag);
        pullVariables.put("buildInfo", providedBuildInfo);

        return pull(pullVariables);
    }

    @Whitelisted
    public BuildInfo pull(Map<String, Object> dockerArguments) throws Exception {
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId);
        dockerArguments.put("credentialsConfig", credentialsConfig);
        dockerArguments.put("host", host);

        return (BuildInfo) script.invokeMethod("dockerPullStep", dockerArguments);
    }
}
