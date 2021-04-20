package org.jfrog.hudson.pipeline.scripted.dsl;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.JfrogServers;

import java.io.Serializable;
import java.util.Map;

public class JfrogPipelineGlobal implements Serializable {
    private final CpsScript cpsScript;
    private JfrogServers jfrogServers;

    public JfrogPipelineGlobal(CpsScript script) {
        this.cpsScript = script;
    }

    @Whitelisted
    public JfrogPipelineGlobal instance(String instanceId) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("jfrogServersID", instanceId);
        jfrogServers = (JfrogServers) cpsScript.invokeMethod("getJfrogServers", stepVariables);
        jfrogServers.setCpsScript(cpsScript);
        jfrogServers.getArtifactoryServer().setCpsScript(cpsScript);
        return this;
    }

    @Whitelisted
    public ArtifactoryServer artifactory() {
        return jfrogServers.getArtifactoryServer();
    }
}
