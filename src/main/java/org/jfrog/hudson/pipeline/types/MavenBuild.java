package org.jfrog.hudson.pipeline.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.MavenResolver;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class MavenBuild implements Serializable {
    private transient CpsScript cpsScript;
    private MavenDeployer deployer = new MavenDeployer();
    private MavenResolver resolver = new MavenResolver();
    private String tool = "";
    private String opts = "";

    public MavenBuild() {
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    @Whitelisted
    public Deployer getDeployer() {
        return deployer;
    }

    @Whitelisted
    public MavenResolver getResolver() {
        return resolver;
    }

    @Whitelisted
    public String getTool() {
        return tool;
    }

    @Whitelisted
    public void setTool(String tool) {
        this.tool = tool;
    }

    @Whitelisted
    public String getOpts() {
        return opts;
    }

    @Whitelisted
    public void setOpts(String opts) {
        this.opts = opts;
    }

    @Whitelisted
    public BuildInfo run(Map<String, Object> args) {
        if (!args.containsKey("goals") || !args.containsKey("pom")) {
            throw new IllegalArgumentException("pom and goals are mandatory arguments.");
        }
        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getExecutionArguments((String) args.get("pom"), (String) args.get("goals"), (BuildInfo) args.get("buildInfo"));
        BuildInfo build = (BuildInfo) cpsScript.invokeMethod("ArtifactoryMavenBuild", stepVariables);
        return build;
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        Set<String> resolverArgumentsSet = resolverArguments.keySet();
        List<String> keysAsList = Arrays.asList("releaseRepo", "snapshotRepo", "server");
        if (!keysAsList.containsAll(resolverArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        // We don't want to handle the deserialization of the ArtifactoryServer.
        // Instead we will remove it and later on set it on the deployer object.
        Object server = resolverArguments.remove("server");
        JSONObject json = new JSONObject();
        json.putAll(resolverArguments);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(this.resolver).readValue(json.toString());
        if (server != null) {
            this.resolver.setServer((ArtifactoryServer) server);
        }
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        Set<String> resolverArgumentsSet = deployerArguments.keySet();
        List<String> keysAsList = Arrays.asList("releaseRepo", "snapshotRepo", "server", "evenIfUnstable", "deployArtifacts", "includeEnvVars");
        if (!keysAsList.containsAll(resolverArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        // We don't want to handle the deserialization of the ArtifactoryServer.
        // Instead we will remove it and later on set it on the deployer object.
        Object server = deployerArguments.remove("server");
        JSONObject json = new JSONObject();
        json.putAll(deployerArguments);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(this.deployer).readValue(json.toString());
        if (server != null) {
            this.deployer.setServer((ArtifactoryServer) server);
        }
    }

    private Map<String, Object> getExecutionArguments(String pom, String goals, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put("mavenBuild", this);
        stepVariables.put("pom", pom);
        stepVariables.put("goals", goals);
        stepVariables.put("buildInfo", buildInfo);
        return stepVariables;
    }
}
