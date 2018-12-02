package org.jfrog.hudson.pipeline.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.NpmDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.NpmResolver;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.hudson.pipeline.Utils.BUILD_INFO;
import static org.jfrog.hudson.pipeline.Utils.appendBuildInfo;

public class NpmBuild implements Serializable {
    private transient CpsScript cpsScript;
    private NpmDeployer deployer = new NpmDeployer();
    private NpmResolver resolver = new NpmResolver();
    private String executablePath = "";

    public NpmBuild() {
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    @Whitelisted
    public NpmDeployer getDeployer() {
        return this.deployer;
    }

    @Whitelisted
    public NpmResolver getResolver() {
        return this.resolver;
    }

    @Whitelisted
    public String getExecutablePath() {
        return executablePath;
    }

    @Whitelisted
    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    @Whitelisted
    public void install(Map<String, Object> args) {
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmInstall", prepareNpmStep(args));
    }

    @Whitelisted
    public void publish(Map<String, Object> args) {
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmPublish", prepareNpmStep(args));
    }

    private Map<String, Object> prepareNpmStep(Map<String, Object> args) {
        Set<String> npmArgumentsSet = args.keySet();
        List<String> keysAsList = Arrays.asList("path", "args", "buildInfo");
        if (!keysAsList.containsAll(npmArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getRunArguments((String) args.get("path"), (String) args.get("args"), (BuildInfo) args.get("buildInfo"));
        appendBuildInfo(cpsScript, stepVariables);
        return stepVariables;
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        Set<String> resolverArgumentsSet = resolverArguments.keySet();
        List<String> keysAsList = Arrays.asList("repo", "server");
        if (!keysAsList.containsAll(resolverArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        // We don't want to handle the deserialization of the ArtifactoryServer.
        // Instead we will remove it and later on set it on the deployer object.
        Object server = resolverArguments.remove("server");
        JSONObject json = new JSONObject();
        json.putAll(resolverArguments);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(resolver).readValue(json.toString());
        if (server != null) {
            resolver.setServer((ArtifactoryServer) server);
        }
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        Set<String> resolverArgumentsSet = deployerArguments.keySet();
        List<String> keysAsList = Arrays.asList("repo", "server", "deployArtifacts", "includeEnvVars");
        if (!keysAsList.containsAll(resolverArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        // We don't want to handle the deserialization of the ArtifactoryServer.
        // Instead we will remove it and later on set it on the deployer object.
        Object server = deployerArguments.remove("server");
        JSONObject json = new JSONObject();
        json.putAll(deployerArguments);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(deployer).readValue(json.toString());
        if (server != null) {
            deployer.setServer((ArtifactoryServer) server);
        }
    }

    private Map<String, Object> getRunArguments(String path, String args, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("npmBuild", this);
        stepVariables.put("path", path);
        stepVariables.put("args", args);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }


}