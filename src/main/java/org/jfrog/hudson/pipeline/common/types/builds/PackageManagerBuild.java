package org.jfrog.hudson.pipeline.common.types.builds;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Yahav Itzhak on 26 Dec 2018.
 */
public class PackageManagerBuild implements Serializable {
    private static final long serialVersionUID = 1L;

    transient CpsScript cpsScript;
    Deployer deployer;
    Resolver resolver;
    String tool = "";

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
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
    public Deployer getDeployer() {
        return deployer;
    }

    @Whitelisted
    public Resolver getResolver() {
        return resolver;
    }

    void setResolver(Map<String, Object> resolverArguments, List<String> keysAsList) throws Exception {
        checkArguments(resolverArguments, keysAsList);
        updateResolverDeployer(resolverArguments, resolver);
        Object server = resolverArguments.get("server");
        if (server != null) {
            resolver.setServer((ArtifactoryServer) server);
        }
    }

    void setDeployer(Map<String, Object> deployerArguments, List<String> keysAsList) throws Exception {
        checkArguments(deployerArguments, keysAsList);
        updateResolverDeployer(deployerArguments, deployer);
        Object server = deployerArguments.remove("server");
        if (server != null) {
            deployer.setServer((ArtifactoryServer) server);
        }
    }

    public void setDeployer(Deployer deployer) {
        this.deployer = deployer;
    }

    public void setResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    private void checkArguments(Map<String, Object> arguments, List<String> keysAsList) {
        Set<String> resolverArgumentsSet = arguments.keySet();
        if (!keysAsList.containsAll(resolverArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }
    }

    private void updateResolverDeployer(Map<String, Object> arguments, Object valueToUpdate) throws IOException {
        // We don't want to handle the deserialization of the ArtifactoryServer.
        // Instead we will remove it and later on set it on the deployer object.
        Map<String, Object> serverlessArguments = new HashMap<>(arguments);
        serverlessArguments.remove("server");
        JSONObject json = new JSONObject();
        json.putAll(serverlessArguments);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(valueToUpdate).readValue(json.toString());
    }
}
