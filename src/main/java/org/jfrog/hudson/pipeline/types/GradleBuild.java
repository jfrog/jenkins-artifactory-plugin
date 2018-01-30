package org.jfrog.hudson.pipeline.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.GradleDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.GradleResolver;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class GradleBuild implements Serializable {
    private transient CpsScript cpsScript;
    private GradleDeployer deployer = new GradleDeployer();
    private GradleResolver resolver = new GradleResolver();
    private String tool = "";
    private boolean useWrapper;
    private boolean usesPlugin;

    public GradleBuild() {
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    @Whitelisted
    public boolean isUsesPlugin() {
        return usesPlugin;
    }

    @Whitelisted
    public void setUsesPlugin(boolean usesPlugin) {
        this.usesPlugin = usesPlugin;
    }

    @Whitelisted
    public GradleDeployer getDeployer() {
        return deployer;
    }

    @Whitelisted
    public GradleResolver getResolver() {
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
    public boolean isUseWrapper() {
        return useWrapper;
    }

    @Whitelisted
    public void setUseWrapper(boolean useWrapper) {
        this.useWrapper = useWrapper;
    }

    @Whitelisted
    public BuildInfo run(Map<String, Object> args) {
        if (!args.containsKey("tasks")) {
            throw new IllegalArgumentException("tasks is a mandatory argument.");
        }
        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getRunArguments((String) args.get("buildFile"), (String) args.get("tasks"), (String) args.get("switches"), (String) args.get("rootDir"), (BuildInfo) args.get("buildInfo"));
        BuildInfo build = (BuildInfo) cpsScript.invokeMethod("ArtifactoryGradleBuild", stepVariables);
        return build;
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
        mapper.readerForUpdating(this.resolver).readValue(json.toString());
        if (server != null) {
            this.resolver.setServer((ArtifactoryServer) server);
        }
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        Set<String> resolverArgumentsSet = deployerArguments.keySet();
        List<String> keysAsList = Arrays.asList("repo", "server", "deployArtifacts", "includeEnvVars", "usesPlugin", "deployMaven", "deployIvy", "ivyPattern", "artifactPattern");
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

    private Map<String, Object> getRunArguments(String buildFile, String tasks, String switches, String rootDir, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put("gradleBuild", this);
        stepVariables.put("rootDir", rootDir);
        stepVariables.put("buildFile", buildFile);
        stepVariables.put("tasks", tasks);
        stepVariables.put("switches", switches);
        stepVariables.put("buildInfo", buildInfo);
        return stepVariables;
    }
}
