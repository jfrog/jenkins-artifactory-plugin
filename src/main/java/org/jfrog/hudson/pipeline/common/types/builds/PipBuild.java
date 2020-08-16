package org.jfrog.hudson.pipeline.common.types.builds;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.resolvers.CommonResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

/**
 * Created by Bar Belity on 07/07/2020.
 */
public class PipBuild extends PackageManagerBuild {

    public static final String PIP_BUILD = "pipBuild";
    public static final String JAVA_ARGS = "javaArgs";
    public static final String ENV_ACTIVATION = "envActivation";

    public PipBuild() {
        resolver = new CommonResolver();
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList(REPO, SERVER));
    }

    @Whitelisted
    public void install(Map<String, Object> args) {
        Map<String, Object> stepVariables = preparePipStep(args, Arrays.asList(JAVA_ARGS, ARGS, BUILD_INFO, ENV_ACTIVATION, MODULE));
        // Throws CpsCallableInvocation - Must be the last line in this method.
        cpsScript.invokeMethod("artifactoryPipRun", stepVariables);
    }

    private Map<String, Object> preparePipStep(Map<String, Object> args, List<String> keysAsList) {
        Set<String> argumentsSet = args.keySet();
        if (!keysAsList.containsAll(argumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }
        Map<String, Object> stepVariables = getPipArguments(
                (String) args.get(ARGS),
                (String) args.get(MODULE),
                (BuildInfo) args.get(BUILD_INFO),
                (String) args.get(JAVA_ARGS),
                (String) args.get(ENV_ACTIVATION));
        appendBuildInfo(cpsScript, stepVariables);
        return stepVariables;
    }

    private Map<String, Object> getPipArguments(String args, String module, BuildInfo buildInfo, String javaArgs, String envActivation) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put(PIP_BUILD, this);
        stepVariables.put(ARGS, args);
        stepVariables.put(MODULE, module);
        stepVariables.put(BUILD_INFO, buildInfo);
        stepVariables.put(JAVA_ARGS, javaArgs);
        stepVariables.put(ENV_ACTIVATION, envActivation);
        return stepVariables;
    }
}
