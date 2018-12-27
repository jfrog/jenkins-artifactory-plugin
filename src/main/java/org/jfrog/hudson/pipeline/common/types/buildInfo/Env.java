package org.jfrog.hudson.pipeline.common.types.buildInfo;

import com.google.common.collect.Maps;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
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
@SuppressWarnings("unused")
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
        addAllWithFilter(envVars, env, filter.getPatternFilter());
        Map<String, String> sysEnv = new HashMap<>();
        Properties systemProperties = System.getProperties();
        Enumeration<?> enumeration = systemProperties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String propertyKey = (String) enumeration.nextElement();
            sysEnv.put(propertyKey, systemProperties.getProperty(propertyKey));
        }
        addAllWithFilter(sysVars, sysEnv, filter.getPatternFilter());
    }

    /**
     * Append environment variables and system properties from othre PipelineEvn object
     */
    protected void append(Env env) {
        addAllWithFilter(this.envVars, env.envVars, filter.getPatternFilter());
        addAllWithFilter(this.sysVars, env.sysVars, filter.getPatternFilter());
    }

    /**
     * Adds all pairs from 'fromMap' to 'toMap' excluding once that matching the pattern
     */
    private void addAllWithFilter(Map<String, String> toMap, Map<String, String> fromMap, IncludeExcludePatterns pattern) {
        for (Object o : fromMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String) entry.getKey();
            if (PatternMatcher.pathConflicts(key, pattern)) {
                continue;
            }
            toMap.put(key, (String) entry.getValue());
        }
    }

    @Whitelisted
    public Map<String, String> getVars() {
        Map<String, String> vars = new HashMap<String, String>();
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
}
