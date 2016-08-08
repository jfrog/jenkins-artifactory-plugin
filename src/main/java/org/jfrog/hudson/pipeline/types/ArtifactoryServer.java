package org.jfrog.hudson.pipeline.types;

import org.codehaus.jackson.map.ObjectMapper;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.CredentialsConfig;

import java.io.Serializable;
import java.util.*;

/**
 * Created by romang on 4/21/16.
 */
public class ArtifactoryServer implements Serializable {
    public static final String BUILD_INFO = "buildInfo";
    public static final String SPEC = "spec";
    public static final String SERVER = "server";
    private String serverName;
    private String url;
    private String username;
    private String password;
    private boolean bypassProxy;
    private CpsScript cpsScript;

    public ArtifactoryServer(String artifactoryServerName, String url, String username, String password) {
        serverName = artifactoryServerName;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public ArtifactoryServer(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public CredentialsConfig createCredentialsConfig() {
        return new CredentialsConfig(this.username, this.password, null, null);
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    @Whitelisted
    public BuildInfo download(String spec) throws Exception {
        return download(spec, null);
    }

    private Map<String, Object> getDownloadUploadObjectMap(Map<String, Object> arguments) {
        if (!arguments.containsKey(SPEC)) {
            throw new IllegalArgumentException(SPEC + " is a mandatory field");
        }

        List<String> keysAsList = Arrays.asList(new String[] {SPEC, BUILD_INFO});
        if (!keysAsList.containsAll(arguments.keySet())) {
            throw new IllegalArgumentException("Only the following arguments are allowed, " + keysAsList.toString());
        }

        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.putAll(arguments);
        stepVariables.put(SERVER, this);
        return stepVariables;
    }

    @Whitelisted
    public BuildInfo download(String spec, BuildInfo providedBuildInfo) throws Exception {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put(SPEC, spec);
        stepVariables.put(BUILD_INFO, providedBuildInfo);
        stepVariables.put(SERVER, this);

        BuildInfo buildInfo = (BuildInfo) cpsScript.invokeMethod("artifactoryDownload", stepVariables);
        buildInfo.setCpsScript(cpsScript);
        return buildInfo;
    }

    @Whitelisted
    public BuildInfo download(Map<String, Object> downloadArguments) throws Exception {
        Map<String, Object> stepVariables = getDownloadUploadObjectMap(downloadArguments);
        BuildInfo buildInfo = (BuildInfo) cpsScript.invokeMethod("artifactoryDownload", stepVariables);
        buildInfo.setCpsScript(cpsScript);
        return buildInfo;
    }

    @Whitelisted
    public BuildInfo upload(String spec) throws Exception {
        return upload(spec, null);
    }

    @Whitelisted
    public BuildInfo upload(Map<String, Object> uploadArguments) throws Exception {
        Map<String, Object> stepVariables = getDownloadUploadObjectMap(uploadArguments);
        BuildInfo buildInfo = (BuildInfo) cpsScript.invokeMethod("artifactoryUpload", stepVariables);
        buildInfo.setCpsScript(cpsScript);
        return buildInfo;
    }

    @Whitelisted
    public BuildInfo upload(String spec, BuildInfo providedBuildInfo) throws Exception {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put(SPEC, spec);
        stepVariables.put(BUILD_INFO, providedBuildInfo);
        stepVariables.put(SERVER, this);

        BuildInfo buildInfo = (BuildInfo) cpsScript.invokeMethod("artifactoryUpload", stepVariables);
        buildInfo.setCpsScript(cpsScript);
        return buildInfo;
    }

    @Whitelisted
    public void publishBuildInfo(BuildInfo buildInfo) throws Exception {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put(BUILD_INFO, buildInfo);
        stepVariables.put(SERVER, this);

        cpsScript.invokeMethod("publishBuildInfo", stepVariables);
    }

    @Whitelisted
    public void promote(Map<String, Object> promotionParams) throws Exception {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put("promotionConfig", createPromotionConfig(promotionParams));
        stepVariables.put(SERVER, this);

        cpsScript.invokeMethod("artifactoryPromoteBuild", stepVariables);
    }

    private PromotionConfig createPromotionConfig(Map<String, Object> promotionParams) {
        final String buildName = "buildName";
        final String buildNumber = "buildNumber";
        final String targetRepository = "targetRepo";
        List<String> mandatoryArgumentsAsList = Arrays.asList(new String[] {buildName, buildNumber, targetRepository});
        if (!promotionParams.keySet().containsAll(mandatoryArgumentsAsList)) {
            throw new IllegalArgumentException(mandatoryArgumentsAsList.toString() +" are mandatory fields");
        }

        Set<String> promotionParamsSet = promotionParams.keySet();
        List<String> keysAsList = Arrays.asList(new String[] {buildName, buildNumber, targetRepository, "sourceRepo", "status", "comment", "includeDependencies", "copy"});
        if (!keysAsList.containsAll(promotionParamsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        final ObjectMapper mapper = new ObjectMapper();
        PromotionConfig config = mapper.convertValue(promotionParams, PromotionConfig.class);

        return config;
    }

    public String getServerName() {
        return serverName;
    }

    @Whitelisted
    public String getUrl() {
        return url;
    }

    @Whitelisted
    public void setUrl(String url) {
        this.url = url;
    }

    @Whitelisted
    public String getUsername() {
        return username;
    }

    @Whitelisted
    public void setUsername(String username) {
        this.username = username;
    }

    @Whitelisted
    public void setPassword(String password){
        this.password = password;
    }

    @Whitelisted
    public void setBypassProxy(boolean bypassProxy) {
        this.bypassProxy = bypassProxy;
    }

    @Whitelisted
    public boolean isBypassProxy() {
        return bypassProxy;
    }

}
