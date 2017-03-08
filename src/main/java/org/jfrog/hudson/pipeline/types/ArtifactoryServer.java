package org.jfrog.hudson.pipeline.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.action.ActionableHelper;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;

import java.io.Serializable;
import java.util.*;

/**
 * Created by romang on 4/21/16.
 */
public class ArtifactoryServer implements Serializable {
    public static final String BUILD_INFO = "buildInfo";
    public static final String SPEC = "spec";
    public static final String SERVER = "server";
    public static final String BUILD_NAME = "buildName";
    public static final String BUILD_NUMBER = "buildNumber";

    private String serverName;
    private String url;
    private String username;
    private String password;
    private String credentialsId;
    private boolean bypassProxy;
    private CpsScript cpsScript;
    private boolean usesCredetialsId;
    private Connection connection = new Connection();

    public ArtifactoryServer() {
    }

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

    public ArtifactoryServer(String url, String credentialsId) {
        this.url = url;
        this.credentialsId = credentialsId;
        this.usesCredetialsId = true;
    }

    public CredentialsConfig createCredentialsConfig() {
        CredentialsConfig credentialsConfig = new CredentialsConfig(this.username, this.password, this.credentialsId, null);
        credentialsConfig.setIgnoreCredentialPluginDisabled(usesCredetialsId);
        return credentialsConfig;
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
            throw new IllegalArgumentException(SPEC + " is a mandatory arguments");
        }

        List<String> keysAsList = Arrays.asList(new String[]{SPEC, BUILD_INFO});
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

    @Whitelisted
    public XrayScanResult xrayScan(Map<String, Object> xrayScanParams) throws Exception {
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put("xrayScanConfig", createXrayScanConfig(xrayScanParams));
        stepVariables.put(SERVER, this);

        return (XrayScanResult) cpsScript.invokeMethod("xrayScanBuild", stepVariables);
    }

    private XrayScanConfig createXrayScanConfig(Map<String, Object> xrayScanParams) {
        final String failBuild = "failBuild";

        List<String> mandatoryArgumentsAsList = Arrays.asList(new String[]{BUILD_NAME, BUILD_NAME});
        if (!xrayScanParams.keySet().containsAll(mandatoryArgumentsAsList)) {
            throw new IllegalArgumentException(mandatoryArgumentsAsList.toString() + " are mandatory arguments");
        }

        Set<String> xrayScanParamsSet = xrayScanParams.keySet();
        List<String> keysAsList = Arrays.asList(new String[]{BUILD_NAME, BUILD_NUMBER, failBuild});
        if (!keysAsList.containsAll(xrayScanParamsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        return new XrayScanConfig((String) xrayScanParams.get(BUILD_NAME),
                (String) xrayScanParams.get(BUILD_NUMBER), (Boolean) xrayScanParams.get(failBuild));
    }

    private PromotionConfig createPromotionConfig(Map<String, Object> promotionParams) {
        final String targetRepository = "targetRepo";
        List<String> mandatoryArgumentsAsList = Arrays.asList(new String[]{BUILD_NAME, BUILD_NUMBER, targetRepository});
        if (!promotionParams.keySet().containsAll(mandatoryArgumentsAsList)) {
            throw new IllegalArgumentException(mandatoryArgumentsAsList.toString() + " are mandatory arguments");
        }

        Set<String> promotionParamsSet = promotionParams.keySet();
        List<String> keysAsList = Arrays.asList(new String[]{BUILD_NAME, BUILD_NUMBER, targetRepository, "sourceRepo", "status", "comment", "includeDependencies", "copy", "failFast"});
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
        this.credentialsId = "";
        this.usesCredetialsId = false;
    }

    @Whitelisted
    public void setPassword(String password) {
        this.password = password;
        this.credentialsId = "";
        this.usesCredetialsId = false;
    }

    public String getPassword() {
        return this.password;
    }

    @Whitelisted
    public void setBypassProxy(boolean bypassProxy) {
        this.bypassProxy = bypassProxy;
    }

    @Whitelisted
    public boolean isBypassProxy() {
        return bypassProxy;
    }

    @Whitelisted
    public String getCredentialsId() {
        return credentialsId;
    }

    @Whitelisted
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        this.password = "";
        this.username = "";
        this.usesCredetialsId = true;
    }

    @Whitelisted
    public Connection getConnection() {
        return connection;
    }

}
