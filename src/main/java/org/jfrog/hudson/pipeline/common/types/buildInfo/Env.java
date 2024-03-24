package org.jfrog.hudson.pipeline.common.types.buildInfo;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.extractor.ci.BuildInfoProperties;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.hudson.pipeline.common.Utils;

import java.io.Serializable;
import java.util.*;

/**
 * Created by romang on 6/22/16.
 */
public class Env implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, String> envVars = new HashMap<>();
    private Map<String, String> sysVars = new HashMap<>();
    private EnvFilter filter = new EnvFilter();
    private boolean capture = false; //By default don't collect
    private transient CpsScript cpsScript;

    private static final String apiKeySecretPrefix = "AKCp8";
    private static final int apiKeySecretMinimalLength = 73;
    // jfrog-ignore
    private static final String referenceTokenSecretPrefix = "cmVmdGtuOjAxOj";
    private static final int referenceTokenSecretMinimalLength = 64;
    // jfrog-ignore
    private static final String accessTokenSecretPrefix = "eyJ2ZXIiOiIyIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJ";
    private static final int accessTokenSecretMinimalLength = 0;

    public Env() {
    }

    /**
     * Collect environment variables and system properties under with filter constrains
     */
    public void collectVariables(EnvVars env, Run build, TaskListener listener) {
        collectVariables(env).collectBuildParameters(build, listener);
    }

    /**
     * Collect environment variables and system properties.
     *
     * @param env - Additional variables to be added on top of the collected system properties and env variables.
     * @return Env which includes system properties, environment variable, and env.
     */
    public Env collectVariables(EnvVars env) {
        this.envVars.putAll(env);
        Map<String, String> sysEnv = new HashMap<>();
        Properties systemProperties = System.getProperties();
        Enumeration<?> enumeration = systemProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String propertyKey = (String) enumeration.nextElement();
            sysEnv.put(propertyKey, systemProperties.getProperty(propertyKey));
        }
        this.sysVars.putAll(sysEnv);
        return this;
    }

    public Env collectBuildParameters(Run<?, ?> build, TaskListener listener) {
        EnvVars buildParameters = Utils.extractBuildParameters(build, listener);
        if (buildParameters != null) {
            this.envVars.putAll(buildParameters);
        }
        return this;
    }

    /**
     * Filter environment variables before publishing the build info.
     *
     * @return the include/exclude pattern
     */
    public IncludeExcludePatterns filter() {
        IncludeExcludePatterns pattern = filter.getPatternFilter();
        filterVars(this.envVars, pattern);
        filterVars(this.sysVars, pattern);
        return pattern;
    }

    /**
     * Filter environment/system variables if their key matches the include/exclude pattern filters, or if their value contains a suspected secret.
     *
     * @param vars    map of environment/system variables
     * @param pattern include/exclude patterns
     */
    private void filterVars(Map<String, String> vars, IncludeExcludePatterns pattern) {
        vars.entrySet().removeIf(key -> PatternMatcher.pathConflicts(key.getKey(), pattern));
        vars.entrySet().removeIf(key -> containsSuspectedSecrets(key.getValue()));
    }

    /**
     * Checks whether the value of a variable contains at least one suspected secret.
     * A suspected secret can be an API-Key, access token or reference token.
     *
     * @param value - string to search in
     * @return whether a secret is suspected
     */
    private boolean containsSuspectedSecrets(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return containsSuspectedSecret(value, apiKeySecretPrefix, apiKeySecretMinimalLength) ||
                containsSuspectedSecret(value, referenceTokenSecretPrefix, referenceTokenSecretMinimalLength) ||
                containsSuspectedSecret(value, accessTokenSecretPrefix, accessTokenSecretMinimalLength);
    }

    /**
     * Checks whether the value of a variable contains a suspected secret.
     * Done by searching for a known constant prefix of the secret and verifying the length of the substring is sufficient to include the expected length of the secret.
     *
     * @param variableValue       - string to search in
     * @param secretPrefix        - secret constant prefix
     * @param secretMinimalLength - secret minimal expected length
     * @return whether a secret is suspected
     */
    private boolean containsSuspectedSecret(String variableValue, String secretPrefix, int secretMinimalLength) {
        int secretIndex = variableValue.indexOf(secretPrefix);
        return secretIndex > -1 && variableValue.substring(secretIndex).length() >= secretMinimalLength;
    }

    /**
     * Append environment variables and system properties from other PipelineEvn object
     */
    public void append(Env env) {
        this.envVars.putAll(env.envVars);
        this.sysVars.putAll(env.sysVars);
    }

    @Whitelisted
    public Map<String, String> getVars() {
        Map<String, String> vars = new HashMap<>();
        vars.putAll(envVars);
        vars.putAll(sysVars);
        return vars;
    }

    @Whitelisted
    public boolean isCapture() {
        return capture;
    }

    @Whitelisted
    public void setCapture(boolean capture) {
        this.capture = capture;
    }

    @Whitelisted
    public void collect() {
        Map<String, Object> stepVariables = new LinkedHashMap<>();
        stepVariables.put("env", this);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("collectEnv", stepVariables);
    }

    @Whitelisted
    public EnvFilter getFilter() {
        return filter;
    }

    public void setFilter(EnvFilter filter) {
        this.filter = filter;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public void setEnvVars(Map<String, String> envVars) {
        this.envVars = envVars;
    }

    public Map<String, String> getSysVars() {
        return sysVars;
    }

    public void setSysVars(Map<String, String> sysVars) {
        this.sysVars = sysVars;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        Map<String, String> envVars = getEnvVars();
        Map<String, String> sysVars = getSysVars();
        if (envVars != null) {
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                properties.put(BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        if (sysVars != null) {
            for (Map.Entry<String, String> entry : sysVars.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }

        return properties;
    }
}
