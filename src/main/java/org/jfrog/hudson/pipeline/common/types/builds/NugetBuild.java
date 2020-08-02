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

public class NugetBuild extends PackageManagerBuild {

    public static final String NUGET_BUILD = "nugetBuild";
    public static final String JAVA_ARGS = "javaArgs";

    private boolean useDotnetCli = false;

    public NugetBuild() {
        resolver = new CommonResolver();
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList(REPO, SERVER));
    }

    @Whitelisted
    public void run(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareNugetStep(args, Arrays.asList(JAVA_ARGS, ARGS, BUILD_INFO, MODULE));
        stepVariables.put(ARGS, args.get(ARGS));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNugetRun", stepVariables);
    }

    public void SetUseDotnetCli(boolean useDotnetCli) {
        this.useDotnetCli = useDotnetCli;
    }

    public boolean useDotnetCli() {
        return useDotnetCli;
    }

    private Map<String, Object> prepareNugetStep(Map<String, Object> args, List<String> keysAsList) {
        Set<String> nugetArgumentsSet = args.keySet();
        if (!keysAsList.containsAll(nugetArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        Map<String, Object> stepVariables = getRunArguments((String) args.get(MODULE), (BuildInfo) args.get(BUILD_INFO));
        appendBuildInfo(cpsScript, stepVariables);
        // Added to allow java remote debugging
        stepVariables.put(JAVA_ARGS, args.get(JAVA_ARGS));
        return stepVariables;
    }


    private Map<String, Object> getRunArguments(String module, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put(NUGET_BUILD, this);
        stepVariables.put(MODULE, module);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }
}
