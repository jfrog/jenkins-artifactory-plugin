package org.jfrog.hudson.pipeline.common.types.buildInfo;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.Issues;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;

import java.io.Serializable;
import java.util.Map;


@SuppressWarnings("unused")
public class TrackedIssues implements Serializable {
    private transient CpsScript cpsScript;
    private Issues issues = new Issues();
    private String buildName;

    public TrackedIssues() {
    }

    // Only interested in appending the issues themselves
    protected void append(TrackedIssues trackedIssuesToAppend) {
        if (trackedIssuesToAppend == null) {
            return;
        }
        if (this.issues == null) {
            this.issues = trackedIssuesToAppend.issues;
            return;
        }
        this.issues.append(trackedIssuesToAppend.issues);
    }

    // Used to invoke the step in a scripted pipeline
    @Whitelisted
    public void collect(ArtifactoryServer server, String config) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("trackedIssues", this);
        stepVariables.put("server", server);
        stepVariables.put("config", config);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("collectIssues", stepVariables);
    }

    @Whitelisted
    public Issues getIssues() {
        return issues;
    }

    public void setIssues(Issues issues) {
        this.issues = issues;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }
}
