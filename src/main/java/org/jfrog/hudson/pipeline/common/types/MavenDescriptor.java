package org.jfrog.hudson.pipeline.common.types;

import org.apache.commons.collections.map.HashedMap;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.extractor.maven.transformer.SnapshotNotAllowedException;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tamirh on 20/11/2016.
 */
public class MavenDescriptor implements Serializable {
    private static final long serialVersionUID = -2403784937804659117L;
    private String pomFile = "pom.xml";
    private String version = "";
    private Map<String, String> versionPerModule = new HashedMap();
    private transient CpsScript cpsScript;
    private boolean failOnSnapshot;


    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    @Whitelisted
    public String getPomFile() {
        return pomFile;
    }

    @Whitelisted
    public void setPomFile(String pomFile) {
        this.pomFile = pomFile;
    }

    @Whitelisted
    public String getVersion() {
        return version;
    }

    @Whitelisted
    public void setVersion(String version) {
        this.version = version;
    }

    @Whitelisted
    public boolean isFailOnSnapshot() {
        return failOnSnapshot;
    }

    @Whitelisted
    public void setFailOnSnapshot(boolean failOnSnapshot) {
        this.failOnSnapshot = failOnSnapshot;
    }

    @Whitelisted
    public MavenDescriptor setVersion(String moduleIdentifier, String moduleVersion) {
        this.versionPerModule.put(moduleIdentifier, moduleVersion);
        return this;
    }

    @Whitelisted
    public Boolean transform() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("pomFile", pomFile);
        params.put("version", this.version);
        params.put("versionPerModule", this.versionPerModule);
        params.put("failOnSnapshot", this.failOnSnapshot);
        params.put("dryRun", false);
        return (Boolean) (this.cpsScript.invokeMethod("MavenDescriptorStep", params));
    }

    @Whitelisted
    public Boolean hasSnapshots() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("pomFile", pomFile);
        // Need some temp version in order to check if we have any snapshot dependecies.
        params.put("version", "1.1.0");
        params.put("versionPerModule", this.versionPerModule);
        params.put("failOnSnapshot", true);
        params.put("dryRun", true);
        try {
            this.cpsScript.invokeMethod("MavenDescriptorStep", params);
        } catch (SnapshotNotAllowedException e) {
            return true;
        }
        return false;
    }
}
