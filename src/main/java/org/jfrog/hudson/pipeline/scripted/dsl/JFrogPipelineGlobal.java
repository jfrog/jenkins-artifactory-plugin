package org.jfrog.hudson.pipeline.scripted.dsl;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.JFrogPlatformInstance;

import java.io.Serializable;
import java.util.Map;

public class JFrogPipelineGlobal implements Serializable {
    private final CpsScript cpsScript;
    private JFrogPlatformInstance JFrogPlatformInstance;

    public JFrogPipelineGlobal(CpsScript script) {
        this.cpsScript = script;
    }

    @Whitelisted
    public JFrogPipelineGlobal instance(String instanceId) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("JFrogPlatformInstanceID", instanceId);
        JFrogPlatformInstance = (JFrogPlatformInstance) cpsScript.invokeMethod("getJFrogPlatformInstance", stepVariables);
        JFrogPlatformInstance.setCpsScript(cpsScript);
        JFrogPlatformInstance.getArtifactoryServer().setCpsScript(cpsScript);
        return this;
    }

    @Whitelisted
    public ArtifactoryServer artifactory() {
        return JFrogPlatformInstance.getArtifactoryServer();
    }
}
