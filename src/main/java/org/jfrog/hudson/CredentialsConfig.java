package org.jfrog.hudson;

import hudson.model.Item;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.plugins.PluginsUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * Configuration for all available credentials providers - Legacy credentials method or credentials plugin
 * Each call for username/password should go through here and this object will provide the username/password
 * according to the global configuration objects which will configured with the "useCredentialsPlugin" property
 *
 * @author Aviad Shikloshi
 */
public class CredentialsConfig implements Serializable {

    public static final CredentialsConfig EMPTY_CREDENTIALS_CONFIG =
            new CredentialsConfig(new Credentials(StringUtils.EMPTY, StringUtils.EMPTY), StringUtils.EMPTY, false);
    private Credentials credentials;
    private String credentialsId;
    private Boolean overridingCredentials;
    private boolean ignoreCredentialPluginDisabled; //We need this for the pipeline flow we can set credentials although the credentials plugin is disabled

    /**
     * Constructed from the build configuration (Maven, Gradle, Ivy, Freestyle, etc)
     * This object obtains the username, password and credentials id (used with the Credentials plugin)
     * Each of these properties could be empty string if not specified but not null
     *
     * @param username      legacy username from textbox
     * @param password      legacy password from textbox
     * @param credentialsId credentialsId chosen from the select box
     */
    @DataBoundConstructor
    public CredentialsConfig(String username, String password, String credentialsId, Boolean overridingCredentials) {
        this.overridingCredentials = overridingCredentials == null ? false : overridingCredentials;
        this.credentials = new Credentials(username, password);
        this.credentialsId = credentialsId;
    }

    public CredentialsConfig(String username, String password, String credentialsId) {
        this.overridingCredentials = false;
        this.ignoreCredentialPluginDisabled = StringUtils.isNotEmpty(credentialsId);
        this.credentials = new Credentials(username, password);
        this.credentialsId = credentialsId;
    }

    public CredentialsConfig(Credentials credentials, String credentialsId, boolean overridingCredentials) {
        this.credentials = credentials;
        this.credentialsId = credentialsId;
        this.overridingCredentials = overridingCredentials;
    }

    public void deleteCredentials() {
        this.credentials = new Credentials(StringUtils.EMPTY, StringUtils.EMPTY);
    }

    /**
     * In case of overriding the global configuration this method should be called to check if override credentials were supplied
     * from configuration - this will take under  consideration the state of the "useCredentialsPlugin" option in global config object
     *
     * s@return in legacy mode this will return true if username and password both supplied
     * In Credentials plugin mode this will return true if a credentials id was selected in the job configuration
     */
    public boolean isCredentialsProvided() {
        if (PluginsUtils.isCredentialsPluginEnabled() || ignoreCredentialPluginDisabled) {
            return StringUtils.isNotBlank(credentialsId);
        }
        return overridingCredentials;
    }

    /**
     * Not like getUsername this will return the username of the current Credentials mode of the system (legacy/credentials plugin)
     *
     * @return the username that should be apply in this configuration
     */
    public String provideUsername(Item item) {
        return isUsingCredentialsPlugin() ? PluginsUtils.credentialsLookup(credentialsId, item).getUsername() :  credentials.getUsername();
    }
    /**
     * Not like getPassword this will return the username of the current Credentials mode of the system (legacy/credentials plugin)
     *
     * @return the password that should be apply in this configuration
     */
    public String providePassword(Item item) {
        return isUsingCredentialsPlugin() ? PluginsUtils.credentialsLookup(credentialsId, item).getPassword() : credentials.getPassword();
    }

    public Credentials getCredentials(Item item) {
        return isUsingCredentialsPlugin() ? PluginsUtils.credentialsLookup(credentialsId, item) : credentials;
    }

    // NOTE: These getters are not part of the API, but used by Jenkins Jelly for displaying values on user interface
    // This should not be used in order to retrieve credentials in the configuration - Use provideUsername, providePassword instead

    public String getUsername() {
        if (credentials == null) {
            return StringUtils.EMPTY;
        }
        return credentials.getUsername();
    }

    public String getPassword() {
        if (credentials == null) {
            return StringUtils.EMPTY;
        }
        return credentials.getPassword();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public boolean isOverridingCredentials() {
        return overridingCredentials;
    }

    public boolean isIgnoreCredentialPluginDisabled() {
        return ignoreCredentialPluginDisabled;
    }

    public void setIgnoreCredentialPluginDisabled(boolean ignoreCredentialPluginDisabled) {
        this.ignoreCredentialPluginDisabled = ignoreCredentialPluginDisabled;
    }

    public boolean isUsingCredentialsPlugin() {
        return (PluginsUtils.isCredentialsPluginEnabled() && StringUtils.isNotEmpty(credentialsId)) || ignoreCredentialPluginDisabled;
    }
}
