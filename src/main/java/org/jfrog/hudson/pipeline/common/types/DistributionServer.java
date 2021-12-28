package org.jfrog.hudson.pipeline.common.types;

import hudson.model.Item;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.DistributionManagerBuilder;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.ProxyUtils;
import org.jfrog.hudson.util.plugins.PluginsUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Represents an instance of Distribution configuration from pipeline script.
 */
public class DistributionServer implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String RELEASE_NOTES_SYNTAX = "releaseNotesSyntax";
    private static final String RELEASE_NOTES_PATH = "releaseNotesPath";
    private static final String SIGN_IMMEDIATELY = "signImmediately";
    private static final String DELETE_FROM_DIST = "deleteFromDist";
    private static final String GPG_PASSPHRASE = "gpgPassphrase";
    private static final String COUNTRY_CODES = "countryCodes";
    private static final String STORING_REPO = "storingRepo";
    private static final String DESCRIPTION = "description";
    private static final String DIST_RULES = "distRules";
    private static final String SITE_NAME = "siteName";
    private static final String CITY_NAME = "cityName";
    private static final String VERSION = "version";
    private static final String DRY_RUN = "dryRun";
    private static final String SERVER = "server";
    private static final String NAME = "name";
    private static final String SPEC = "spec";
    private static final String SYNC = "sync";

    private static final List<String> CREATE_UPDATE_MANDATORY_ARGS = createArgsList(NAME, VERSION, SPEC);
    private static final List<String> CREATE_UPDATE_OPTIONAL_ARGS =
            createArgsList(DRY_RUN, SIGN_IMMEDIATELY, GPG_PASSPHRASE, RELEASE_NOTES_PATH, RELEASE_NOTES_SYNTAX, STORING_REPO, DESCRIPTION);

    private static final List<String> SIGN_MANDATORY_ARGS = createArgsList(NAME, VERSION);
    private static final List<String> SIGN_OPTIONAL_ARGS = createArgsList(GPG_PASSPHRASE, STORING_REPO);

    private static final List<String> DISTRIBUTE_DELETE_MANDATORY_ARGS = createArgsList(NAME, VERSION);
    private static final List<String> DISTRIBUTE_OPTIONAL_ARGS = createArgsList(DRY_RUN, SYNC, DIST_RULES, COUNTRY_CODES, CITY_NAME, SITE_NAME);

    private static final List<String> DELETE_OPTIONAL_ARGS = createArgsList(DRY_RUN, SYNC, DIST_RULES, COUNTRY_CODES, CITY_NAME, SITE_NAME, DELETE_FROM_DIST);

    private final Connection connection = new Connection();
    private transient CpsScript cpsScript;
    private String id;
    private String url;
    private String username;
    private String password;
    private String credentialsId;
    private boolean bypassProxy;
    private boolean usesCredentialsId;

    private static List<String> createArgsList(String... args) {
        return Arrays.asList(args);
    }

    public DistributionServer() {
    }

    public DistributionServer(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public DistributionServer(String url, String credentialsId) {
        this.url = url;
        this.credentialsId = credentialsId;
        this.usesCredentialsId = true;
    }

    public DistributionServer(org.jfrog.hudson.JFrogPlatformInstance jfrogPlatformInstance, Item parent) {
        id = jfrogPlatformInstance.getId();
        url = jfrogPlatformInstance.getDistributionUrl();
        if (PluginsUtils.isCredentialsPluginEnabled()) {
            credentialsId = jfrogPlatformInstance.getDeployerCredentialsConfig().getCredentialsId();
        } else {
            Credentials serverCredentials = jfrogPlatformInstance.getDeployerCredentialsConfig().provideCredentials(parent);
            username = serverCredentials.getUsername();
            password = serverCredentials.getPassword();
        }
        bypassProxy = jfrogPlatformInstance.isBypassProxy();
        connection.setRetry(jfrogPlatformInstance.getConnectionRetry());
        connection.setTimeout(jfrogPlatformInstance.getTimeout());
    }

    @Whitelisted
    public String getUrl() {
        return url;
    }

    @Whitelisted
    public String getUsername() {
        return username;
    }

    @Whitelisted
    public String getPassword() {
        return password;
    }

    @Whitelisted
    public void setUrl(String url) {
        this.url = url;
    }

    @Whitelisted
    public void setUsername(String username) {
        this.username = username;
        this.credentialsId = "";
        this.usesCredentialsId = false;
    }

    @Whitelisted
    public void setPassword(String password) {
        this.password = password;
        this.credentialsId = "";
        this.usesCredentialsId = false;
    }

    @Whitelisted
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        this.password = "";
        this.username = "";
        this.usesCredentialsId = true;
    }

    public CredentialsConfig createCredentialsConfig() {
        CredentialsConfig credentialsConfig = new CredentialsConfig(username, password, credentialsId, null);
        credentialsConfig.setIgnoreCredentialPluginDisabled(usesCredentialsId);
        return credentialsConfig;
    }

    public DistributionManagerBuilder createDistributionManagerBuilder(Log log, Item parent) {
        Credentials credentials = createCredentialsConfig().provideCredentials(parent);
        DistributionManagerBuilder builder = new DistributionManagerBuilder()
                .setServerUrl(url)
                .setUsername(credentials.getUsername())
                .setPassword(credentials.getPassword())
                .setLog(log)
                .setConnectionRetry(connection.getRetry())
                .setConnectionTimeout(connection.getTimeout());
        if (!bypassProxy) {
            builder.setProxyConfiguration(ProxyUtils.createProxyConfiguration());
        }
        return builder;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    @Whitelisted
    public void createReleaseBundle(Map<String, Object> params) {
        Map<String, Object> stepVariables = createObjectMap(params, CREATE_UPDATE_MANDATORY_ARGS, CREATE_UPDATE_OPTIONAL_ARGS);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("createReleaseBundle", stepVariables);
    }

    @Whitelisted
    public void createReleaseBundle(String name, String version, String spec, boolean dryRun, boolean signImmediately, String storingRepo,
                                    String gpgPassphrase, String releaseNotesPath, String releaseNotesSyntax, String description) {
        createReleaseBundle(createUpdateParams(name, version, spec, dryRun, signImmediately, gpgPassphrase, storingRepo, releaseNotesPath, releaseNotesSyntax, description));
    }

    @Whitelisted
    public void updateReleaseBundle(Map<String, Object> params) {
        Map<String, Object> stepVariables = createObjectMap(params, CREATE_UPDATE_MANDATORY_ARGS, CREATE_UPDATE_OPTIONAL_ARGS);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("updateReleaseBundle", stepVariables);
    }

    @Whitelisted
    public void updateReleaseBundle(String name, String version, String spec, boolean dryRun, boolean signImmediately, String storingRepo,
                                    String gpgPassphrase, String releaseNotesPath, String releaseNotesSyntax, String description) {
        updateReleaseBundle(createUpdateParams(name, version, spec, dryRun, signImmediately, gpgPassphrase, storingRepo, releaseNotesPath, releaseNotesSyntax, description));
    }

    @Whitelisted
    public void signReleaseBundle(Map<String, Object> params) {
        Map<String, Object> stepVariables = createObjectMap(params, SIGN_MANDATORY_ARGS, SIGN_OPTIONAL_ARGS);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("signReleaseBundle", stepVariables);
    }

    @Whitelisted
    public void signReleaseBundle(String name, String version, String gpgPassphrase, String storingRepo) {
        signReleaseBundle(createSignParams(name, version, gpgPassphrase, storingRepo));
    }

    @Whitelisted
    public void distributeReleaseBundle(Map<String, Object> params) {
        Map<String, Object> stepVariables = createObjectMap(params, DISTRIBUTE_DELETE_MANDATORY_ARGS, DISTRIBUTE_OPTIONAL_ARGS);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("distributeReleaseBundle", stepVariables);
    }

    @Whitelisted
    public void distributeReleaseBundle(String name, String version, boolean dryRun, boolean sync,
                                        String distRules, List<String> countryCodes, String siteName, String cityName) {
        distributeReleaseBundle(createDistributeParams(name, version, dryRun, sync, distRules, countryCodes, siteName, cityName));
    }

    @Whitelisted
    public void deleteReleaseBundle(Map<String, Object> params) {
        Map<String, Object> stepVariables = createObjectMap(params, DISTRIBUTE_DELETE_MANDATORY_ARGS, DELETE_OPTIONAL_ARGS);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("deleteReleaseBundle", stepVariables);
    }

    @Whitelisted
    public void deleteReleaseBundle(String name, String version, boolean dryRun, boolean sync, boolean deleteFromDist,
                                    String distRules, List<String> countryCodes, String siteName, String cityName) {
        Map<String, Object> params = createDistributeParams(name, version, dryRun, sync, distRules, countryCodes, siteName, cityName);
        params.put(DELETE_FROM_DIST, deleteFromDist);
        deleteReleaseBundle(params);
    }

    private Map<String, Object> createUpdateParams(String name, String version, String spec, boolean dryRun,
                                                   boolean signImmediately, String gpgPassphrase, String storingRepo,
                                                   String releaseNotesPath, String releaseNotesSyntax, String description) {
        Map<String, Object> params = createCommonParams(name, version);
        params.put(SPEC, spec);
        params.put(DRY_RUN, dryRun);
        params.put(SIGN_IMMEDIATELY, signImmediately);
        params.put(GPG_PASSPHRASE, gpgPassphrase);
        params.put(STORING_REPO, storingRepo);
        params.put(RELEASE_NOTES_PATH, releaseNotesPath);
        params.put(RELEASE_NOTES_SYNTAX, releaseNotesSyntax);
        params.put(DESCRIPTION, description);
        return params;
    }

    private Map<String, Object> createSignParams(String name, String version, String gpgPassphrase, String storingRepo) {
        Map<String, Object> params = createCommonParams(name, version);
        params.put(GPG_PASSPHRASE, gpgPassphrase);
        params.put(STORING_REPO, storingRepo);
        return params;
    }

    private Map<String, Object> createDistributeParams(String name, String version, boolean dryRun, boolean sync,
                                                       String distRules, List<String> countryCodes, String siteName, String cityName) {
        Map<String, Object> params = createCommonParams(name, version);
        params.put(DRY_RUN, dryRun);
        params.put(SYNC, sync);
        params.put(DIST_RULES, distRules);
        params.put(COUNTRY_CODES, countryCodes);
        params.put(SITE_NAME, siteName);
        params.put(CITY_NAME, cityName);
        return params;
    }

    private Map<String, Object> createCommonParams(String name, String version) {
        return new HashMap<String, Object>() {{
            put(NAME, name);
            put(VERSION, version);
        }};
    }

    private Map<String, Object> createObjectMap(Map<String, Object> actualArgs, List<String> mandatory, List<String> optional) {
        if (!actualArgs.keySet().containsAll(mandatory)) {
            throw new IllegalArgumentException("The following parameters are mandatory: " + String.join(", ", mandatory));
        }
        Set<String> allArgs = new HashSet<>(mandatory);
        allArgs.addAll(optional);
        if (!allArgs.containsAll(actualArgs.keySet())) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + String.join(", ", allArgs));
        }
        Map<String, Object> stepVariables = new LinkedHashMap<>(actualArgs);
        stepVariables.put(SERVER, this);
        return stepVariables;
    }
}
