package org.jfrog.hudson.pipeline.common.types;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;

import java.io.Serializable;
import java.util.Map;

import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

public class ConanClient implements Serializable {
    public final static String CONAN_LOG_FILE = "conan_log.log";
    private transient CpsScript cpsScript;
    private String userPath;
    private boolean unixAgent;
    private ConanRemote remote = new ConanRemote();

    public ConanClient() {
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
        this.remote.setCpsScript(cpsScript);
    }

    public void setUnixAgent(boolean unixAgent) {
        this.unixAgent = unixAgent;
    }

    @Whitelisted
    public String getUserPath() {
        return userPath;
    }

    public void setUserPath(String conanHomePath) {
        this.userPath = conanHomePath;
        this.remote.setConanHome(conanHomePath);
    }

    @Whitelisted
    public ConanRemote getRemote() {
        return this.remote;
    }

    public void setRemote(ConanRemote remote) {
        this.remote = remote;
    }

    @Whitelisted
    public void run(Map<String, Object> args) {
        if (!args.containsKey("command")) {
            throw new IllegalArgumentException("'command' is a mandatory argument.");
        }
        String command = (String) args.get("command");
        Map<String, Object> stepVariables = getRunCommandExecutionArguments(command, (BuildInfo) args.get("buildInfo"));
        appendBuildInfo(cpsScript, stepVariables);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("runConanCommand", stepVariables);
    }

    private Map<String, Object> getRunCommandExecutionArguments(String command, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("command", command);
        stepVariables.put("conanHome", getUserPath());
        stepVariables.put("buildInfo", buildInfo);
        return stepVariables;
    }
}