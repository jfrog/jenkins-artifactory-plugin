package org.jfrog.hudson.pipeline.common.types;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import static org.jfrog.hudson.pipeline.common.Utils.BUILD_INFO;
import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

/**
 * Created by romang on 7/28/16.
 */
public class Docker implements Serializable {
    private transient CpsScript cpsScript;
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
        this.cpsScript = script;
        this.username = username;
        this.password = password;
        this.credentialsId = credentialsId;
        this.host = host;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
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
    public void push(String imageTag, String targetRepository) {
        push(imageTag, targetRepository, null);
    }

    @Whitelisted
    public void push(String imageTag, String targetRepository, BuildInfo providedBuildInfo) {
        Map<String, Object> dockerArguments = Maps.newLinkedHashMap();
        dockerArguments.put("image", imageTag);
        dockerArguments.put("targetRepo", targetRepository);
        dockerArguments.put(BUILD_INFO, providedBuildInfo);
        push(dockerArguments);
    }

    @Whitelisted
    public void push(Map<String, Object> dockerArguments) {
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId);
        dockerArguments.put("credentialsConfig", credentialsConfig);
        dockerArguments.put("host", host);
        dockerArguments.put("properties", properties);
        dockerArguments.put("server", server);
        appendBuildInfo(cpsScript, dockerArguments);

        if (server != null) {
            // Throws CpsCallableInvocation - Must be the last line in this method
            cpsScript.invokeMethod("dockerPushStep", dockerArguments);
        } else {
            // Deprecated docker push step using proxy
            // Throws CpsCallableInvocation - Must be the last line in this method
            cpsScript.invokeMethod("dockerPushWithProxyStep", dockerArguments);
        }
    }

    @Whitelisted
    public void pull(String imageTag) {
        pull(imageTag, null);
    }

    @Whitelisted
    public void pull(String imageTag, BuildInfo providedBuildInfo) {
        Map<String, Object> pullVariables = Maps.newLinkedHashMap();
        pullVariables.put("image", imageTag);
        pullVariables.put(BUILD_INFO, providedBuildInfo);
        pull(pullVariables);
    }

    @Whitelisted
    public void pull(Map<String, Object> dockerArguments) {
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId);
        dockerArguments.put("credentialsConfig", credentialsConfig);
        dockerArguments.put("host", host);
        appendBuildInfo(cpsScript, dockerArguments);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("dockerPullStep", dockerArguments);
    }
}
