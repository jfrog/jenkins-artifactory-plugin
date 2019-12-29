package org.jfrog.hudson.pipeline.common.types.builds;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmGoDeployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.NpmGoResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.hudson.pipeline.common.Utils.BUILD_INFO;
import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

public class GoBuild extends PackageManagerBuild {

    public GoBuild() {
        deployer = new NpmGoDeployer();
        resolver = new NpmGoResolver();
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList("repo", "server"));
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        setDeployer(deployerArguments, Arrays.asList("repo", "server", "includeEnvVars"));
    }

    @Whitelisted
    public void run(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareGoRunStep(args, Arrays.asList("path", "args", "buildInfo"));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryGoRun", stepVariables);
    }

    @Whitelisted
    public void publish(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareGoPublishStep(args, Arrays.asList("path", "version", "buildInfo"));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryGoPublish", stepVariables);
    }

    private Map<String, Object> prepareGoPublishStep(Map<String, Object> args, List<String> keysAsList) {
        Set<String> argumentsSet = args.keySet();
        if (!keysAsList.containsAll(argumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getPublishArguments((String) args.get("path"),
                (String) args.get("version"),
                (BuildInfo) args.get("buildInfo"));
        appendBuildInfo(cpsScript, stepVariables);
        return stepVariables;
    }

    private Map<String, Object> getPublishArguments(String path, String version, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("goBuild", this);
        stepVariables.put("path", path);
        stepVariables.put("version", version);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }

    private Map<String, Object> prepareGoRunStep(Map<String, Object> args, List<String> keysAsList) {
        Set<String> argumentsSet = args.keySet();
        if (!keysAsList.containsAll(argumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getRunArguments((String) args.get("path"),
                (String) args.get("args"),
                (BuildInfo) args.get("buildInfo"));
        appendBuildInfo(cpsScript, stepVariables); // todo take output
        return stepVariables;
    }

    private Map<String, Object> getRunArguments(String path, String args, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("goBuild", this);
        stepVariables.put("path", path);
        stepVariables.put("goCmdArgs", args);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }
}
