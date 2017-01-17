package org.jfrog.hudson.pipeline.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.types.*;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Tamirh on 18/05/2016.
 */
public class ArtifactoryPipelineGlobal implements Serializable {
    private org.jenkinsci.plugins.workflow.cps.CpsScript script;

    public ArtifactoryPipelineGlobal(CpsScript script) {
        this.script = script;
    }

    @Whitelisted
    public ArtifactoryServer server(String serverName) {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put("artifactoryServerID", serverName);
        ArtifactoryServer server = (ArtifactoryServer) this.script.invokeMethod("getArtifactoryServer", stepVariables);
        server.setCpsScript(this.script);
        return server;
    }

    @Whitelisted
    public Docker docker(String username, String password) {
        return new Docker(script, username, password, null, null);
    }

    @Whitelisted
    public Docker docker(String username, String password, String host) {
        return new Docker(script, username, password, null, host);
    }

    @Whitelisted
    public Docker docker() {
        return new Docker(script, null, null, null, null);
    }

    @Whitelisted
    public Docker docker(Map<String, Object> dockerArguments) {
        List<String> keysAsList = Arrays.asList(new String[]{"username", "password", "credentialsId", "host"});
        if (!keysAsList.containsAll(dockerArguments.keySet())) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList);
        }

        final ObjectMapper mapper = new ObjectMapper();
        Docker docker = mapper.convertValue(dockerArguments, Docker.class);
        docker.setCpsScript(script);
        return docker;
    }


    @Whitelisted
    public ArtifactoryServer newServer(String url, String username, String password) {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put("url", url);
        stepVariables.put("username", username);
        stepVariables.put("password", password);
        ArtifactoryServer server = (ArtifactoryServer) this.script.invokeMethod("newArtifactoryServer", stepVariables);
        server.setCpsScript(this.script);
        return server;
    }

    @Whitelisted
    public ArtifactoryServer newServer(Map<String, Object> serverArguments) {
        List<String> keysAsList = Arrays.asList(new String[]{"url", "username", "password", "credentialsId"});
        if (!keysAsList.containsAll(serverArguments.keySet())) {
            throw new IllegalArgumentException("The newServer method accepts the following arguments only: " + keysAsList);
        }

        ArtifactoryServer server = (ArtifactoryServer) this.script.invokeMethod("newArtifactoryServer", serverArguments);
        server.setCpsScript(this.script);
        return server;
    }

    @Whitelisted
    public BuildInfo newBuildInfo() {
        BuildInfo buildInfo = (BuildInfo) this.script.invokeMethod("newBuildInfo", new LinkedHashMap<String, Object>());
        buildInfo.setCpsScript(this.script);
        return buildInfo;
    }

    @Whitelisted
    public MavenBuild newMavenBuild() {
        MavenBuild mavenBuild = (MavenBuild) this.script.invokeMethod("newMavenBuild", new LinkedHashMap<String, Object>());
        mavenBuild.setCpsScript(this.script);
        return mavenBuild;
    }

    @Whitelisted
    public GradleBuild newGradleBuild() {
        GradleBuild gradleBuild = (GradleBuild) this.script.invokeMethod("newGradleBuild", new LinkedHashMap<String, Object>());
        gradleBuild.setCpsScript(this.script);
        return gradleBuild;
    }

    @Whitelisted
    public MavenDescriptor mavenDescriptor() {
        MavenDescriptor descriptorHandler = new MavenDescriptor();
        descriptorHandler.setCpsScript(this.script);
        return descriptorHandler;
    }
}
