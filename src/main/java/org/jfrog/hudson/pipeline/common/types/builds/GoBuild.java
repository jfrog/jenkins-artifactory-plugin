package org.jfrog.hudson.pipeline.common.types.builds;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmGoDeployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

public class GoBuild extends PackageManagerBuild {

    public static final String GO_BUILD = "goBuild";
    public static final String GO_CMD_ARGS = "goCmdArgs";
    public static final String VERSION = "version";

    public GoBuild() {
        deployer = new NpmGoDeployer();
        resolver = new CommonResolver();
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList(REPO, SERVER));
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        setDeployer(deployerArguments, Arrays.asList(REPO, SERVER, INCLUDE_ENV_VARS));
    }

    @Whitelisted
    public void run(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareGoStep(args, Arrays.asList(PATH, ARGS, BUILD_INFO, MODULE));
        stepVariables.put(GO_CMD_ARGS, args.get(ARGS));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryGoRun", stepVariables);
    }

    @Whitelisted
    public void publish(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareGoStep(args, Arrays.asList(PATH, VERSION, BUILD_INFO, MODULE));
        stepVariables.put(VERSION, args.get(VERSION));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryGoPublish", stepVariables);
    }

    private Map<String, Object> prepareGoStep(Map<String, Object> args, List<String> keysAsList) {
        Set<String> argumentsSet = args.keySet();
        if (!keysAsList.containsAll(argumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getGoArguments((String) args.get(PATH),
                (String) args.get(MODULE),
                (BuildInfo) args.get(BUILD_INFO));
        appendBuildInfo(cpsScript, stepVariables);
        return stepVariables;
    }

    private Map<String, Object> getGoArguments(String path, String module, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put(GO_BUILD, this);
        stepVariables.put(PATH, path);
        stepVariables.put(MODULE, module);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }
}
