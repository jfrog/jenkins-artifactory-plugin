package org.jfrog.hudson.pipeline.common.types;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.hudson.pipeline.common.Utils.BUILD_INFO;
import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

/**
 * Created by romang on 7/28/16.
 */
public class Docker implements Serializable {
    private transient CpsScript cpsScript;
    private String host;
    private String javaArgs;
    // Properties to attach to the deployed docker layers.
    private ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
    private ArtifactoryServer server;

    public Docker() {
    }

    public Docker(CpsScript script, String host, String javaArgs) {
        this.cpsScript = script;
        this.host = host;
        this.javaArgs = javaArgs;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
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
        dockerArguments.put("host", host);
        dockerArguments.put("properties", properties);
        dockerArguments.put("server", server);
        dockerArguments.put("javaArgs", javaArgs);
        appendBuildInfo(cpsScript, dockerArguments);
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("dockerPushStep", dockerArguments);
    }

    @Whitelisted
    public void pull(String imageTag) {
        pull(imageTag, null);
    }

    @Whitelisted
    public void pull(String imageTag, BuildInfo providedBuildInfo) {
        Map<String, Object> dockerArguments = Maps.newLinkedHashMap();
        dockerArguments.put("image", imageTag);
        dockerArguments.put(BUILD_INFO, providedBuildInfo);
        pull(dockerArguments);
    }

    @Whitelisted
    public void pull(Map<String, Object> dockerArguments) {
        dockerArguments.put("host", host);
        dockerArguments.put("server", server);
        dockerArguments.put("javaArgs", javaArgs);
        appendBuildInfo(cpsScript, dockerArguments);
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("dockerPullStep", dockerArguments);
    }
}
