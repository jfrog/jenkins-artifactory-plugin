package org.jfrog.hudson.pipeline.types.buildInfo;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.hudson.pipeline.Utils;

import java.io.Serializable;
import java.util.*;

/**
 * Created by romang on 6/22/16.
 */
public class Env implements Serializable {
    private Map<String, String> envVars = new HashMap<String, String>();
    private Map<String, String> sysVars = new HashMap<String, String>();
    private EnvFilter filter = new EnvFilter();
    private boolean capture = false; //By default don't collect

    private CpsScript cpsScript;

    public Env() {
    }


    /**
     * Collect environment variables and system properties under with filter constrains
     *
     * @param env
     * @param build
     * @param listener
     * @throws Exception
     */
    public void collectVariables(EnvVars env, Run build, TaskListener listener) throws Exception {
        env.putAll(Utils.extractBuildParameters(build, listener));
        addAllWithFilter(envVars, env, filter.getPatternFilter());

        Map<String, String> sysEnv = new HashMap<String, String>();
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
     *
     * @param env
     */
    protected void append(Env env) {
        addAllWithFilter(this.envVars, env.envVars, filter.getPatternFilter());
        addAllWithFilter(this.sysVars, env.sysVars, filter.getPatternFilter());
    }

    /**
     * Adds all pairs from 'fromMap' to 'toMap' excluding once that matching the pattern
     *
     * @param toMap
     * @param fromMap
     * @param pattern
     */
    private void addAllWithFilter(Map<String, String> toMap, Map<String, String> fromMap, IncludeExcludePatterns pattern) {
        Iterator entries = fromMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
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
        Map<String, Object> stepVariables = new LinkedHashMap<String, Object>();
        stepVariables.put("env", this);
        cpsScript.invokeMethod("collectEnv", stepVariables);
    }

    @Whitelisted
    public EnvFilter getFilter() {
        return filter;
    }

    protected Map<String, String> getEnvVars() {
        return envVars;
    }

    protected Map<String, String> getSysVars() {
        return sysVars;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }
}
