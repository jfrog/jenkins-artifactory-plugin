package org.jfrog.hudson;

import hudson.model.Item;
import hudson.util.Secret;
import hudson.util.XStream2;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.converters.CredentialsConfigConverter;
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
            new CredentialsConfig(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, false);
    private Secret username;
    private Secret password;
    private String credentialsId;
    private Boolean overridingCredentials;
    private boolean ignoreCredentialPluginDisabled; //We need this for the pipeline flow we can set credentials although the credentials plugin is disabled

    /**
     * @deprecated: Use org.jfrog.hudson.CredentialsConfig#username and org.jfrog.hudson.CredentialsConfig#password
     * Convert by org.jfrog.hudson.util.converters.CredentialsConfigConverter
     */
    @Deprecated
    private Credentials credentials = null;

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
        if (overridingCredentials == null || overridingCredentials.equals(Boolean.TRUE)) {
            this.username = Secret.fromString(username);
            this.password = Secret.fromString(password);
        }
        this.credentialsId = credentialsId;
    }

    public CredentialsConfig(String username, String password, String credentialsId) {
        this.overridingCredentials = false;
        this.ignoreCredentialPluginDisabled = StringUtils.isNotEmpty(credentialsId);
        this.username = Secret.fromString(username);
        this.password = Secret.fromString(password);
        this.credentialsId = credentialsId;
    }

    public void deleteCredentials() {
        this.username = Secret.fromString(StringUtils.EMPTY);
        this.password = Secret.fromString(StringUtils.EMPTY);
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
    private String provideUsername(Item item) {
        return isUsingCredentialsPlugin() ? PluginsUtils.usernamePasswordCredentialsLookup(credentialsId, item).getUsername() : Secret.toString(username);
    }
    /**
     * Not like getPassword this will return the password of the current Credentials mode of the system (legacy/credentials plugin)
     *
     * @return the password that should be apply in this configuration
     */
    private String providePassword(Item item) {
        return isUsingCredentialsPlugin() ? PluginsUtils.usernamePasswordCredentialsLookup(credentialsId, item).getPassword() : Secret.toString(password);
    }

    public String provideAccessToken(Item item) {
        if (isUsingCredentialsPlugin()) {
            StringCredentialsImpl accessTokenCredentials = PluginsUtils.accessTokenCredentialsLookup(credentialsId, item);
            if (accessTokenCredentials != null) {
                return accessTokenCredentials.getSecret().getPlainText();
            }
        }
        return StringUtils.EMPTY;
    }

    public Credentials provideCredentials(Item item) {
        String accessToken = provideAccessToken(item);
        if (StringUtils.isNotEmpty(accessToken)) {
            return new Credentials(accessToken);
        }

        return new Credentials(provideUsername(item), providePassword(item));
    }

    // NOTE: These getters are not part of the API, but used by Jenkins Jelly for displaying values on user interface
    // This should not be used in order to retrieve credentials in the configuration - Use provideUsername, providePassword instead

    public String getUsername() {
        return Secret.toString(username);
    }

    public Secret getPassword() {
        return password;
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

    /**
     * Page Converter
     */
    public static final class ConverterImpl extends CredentialsConfigConverter {
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
        }
    }
}
