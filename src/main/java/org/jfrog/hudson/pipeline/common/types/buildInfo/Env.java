package org.jfrog.hudson.pipeline.common.types.buildInfo;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.hudson.pipeline.common.Utils;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by romang on 6/22/16.
 */
public class Env implements Serializable {
    private Map<String, String> envVars = new HashMap<>();
    private Map<String, String> sysVars = new HashMap<>();
    private EnvFilter filter = new EnvFilter();
    private boolean capture = false; //By default don't collect
    private transient CpsScript cpsScript;

    public Env() {
    }

    /**
     * Collect environment variables and system properties under with filter constrains
     */
    public void collectVariables(EnvVars env, Run build, TaskListener listener) {
        EnvVars buildParameters = Utils.extractBuildParameters(build, listener);
        if (buildParameters != null) {
            env.putAll(buildParameters);
        }
        this.envVars.putAll(env);
        Map<String, String> sysEnv = new HashMap<>();
        Properties systemProperties = System.getProperties();
        Enumeration<?> enumeration = systemProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String propertyKey = (String) enumeration.nextElement();
            sysEnv.put(propertyKey, systemProperties.getProperty(propertyKey));
        }
        this.sysVars.putAll(sysEnv);
    }

    /**
     * Filter environment variable before publish the build info
     */
    public void filter() {
        IncludeExcludePatterns pattern = filter.getPatternFilter();
        this.envVars.entrySet().removeIf(key -> PatternMatcher.pathConflicts(key.getKey(), pattern));
        this.sysVars.entrySet().removeIf(key -> PatternMatcher.pathConflicts(key.getKey(), pattern));
    }

    /**
     * Append environment variables and system properties from othre PipelineEvn object
     */
    protected void append(Env env) {
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
    public void setCapture(boolean capture) {
        this.capture = capture;
    }

    @Whitelisted
    public boolean isCapture() {
        return capture;
    }

    @Whitelisted
    public void collect() {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("env", this);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("collectEnv", stepVariables);
    }

    public void setFilter(EnvFilter filter) {
        this.filter = filter;
    }

    @Whitelisted
    public EnvFilter getFilter() {
        return filter;
    }

    public void setEnvVars(Map<String, String> envVars) {
        this.envVars = envVars;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public void setSysVars(Map<String, String> sysVars) {
        this.sysVars = sysVars;
    }

    public Map<String, String> getSysVars() {
        return sysVars;
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
