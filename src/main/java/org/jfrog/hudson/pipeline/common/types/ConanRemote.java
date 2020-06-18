package org.jfrog.hudson.pipeline.common.types;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * Created by tamirh on 15/03/2017.
 */

public class ConanRemote implements Serializable {
    private transient CpsScript cpsScript;
    private String conanHome;

    public ConanRemote() {
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    public void setConanHome(String conanHome) {
        this.conanHome = conanHome;
    }

    @Whitelisted
    public String add(Map<String, Object> args) {
        if (!args.containsKey("server") || !args.containsKey("repo")) {
            throw new IllegalArgumentException("server and repo are mandatory arguments.");
        }
        ArtifactoryServer server = (ArtifactoryServer) args.get("server");
        String serverName = args.containsKey("remoteName") ? args.get("remoteName").toString() : UUID.randomUUID().toString();
        String repo = (String) args.get("repo");
        boolean force = args.containsKey("force") && (boolean) args.get("force");
        boolean verifySSL = args.containsKey("verifySSL") ? (boolean) args.get("verifySSL") : true;
        cpsScript.invokeMethod("conanAddRemote", getAddRemoteExecutionArguments(server, serverName, repo, force, verifySSL));
        cpsScript.invokeMethod("conanAddUser", getAddUserExecutionArguments(server, serverName));
        return serverName;
    }

    private Map<String, Object> getAddRemoteExecutionArguments(ArtifactoryServer server, String serverName, String repo, boolean force, boolean verifySSL) {
        String serverUrl = buildConanRemoteUrl(server, repo);
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("serverUrl", serverUrl);
        stepVariables.put("serverName", serverName);
        stepVariables.put("conanHome", conanHome);
        stepVariables.put("force", force);
        stepVariables.put("verifySSL", verifySSL);
        return stepVariables;
    }

    private String buildConanRemoteUrl(ArtifactoryServer server, String repo) {
        StringBuilder serverURL = new StringBuilder(server.getUrl());
        if (!StringUtils.endsWith(serverURL.toString(), "/")) {
            serverURL.append("/");
        }
        serverURL.append("api/conan/").append(repo);
        return serverURL.toString();
    }

    private Map<String, Object> getAddUserExecutionArguments(ArtifactoryServer server, String serverName) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("server", server);
        stepVariables.put("serverName", serverName);
        stepVariables.put("conanHome", conanHome);
        return stepVariables;
    }
}