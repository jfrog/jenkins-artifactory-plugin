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

/**
 * Created by Yahav Itzhak on 26 Dec 2018.
 */
public class NpmBuild extends PackageManagerBuild {

    public static final String NPM_BUILD = "npmBuild";
    public static final String JAVA_ARGS = "javaArgs";
    public static final String DEPLOY_ARTIFACTS = "deployArtifacts";

    public NpmBuild() {
        deployer = new NpmGoDeployer();
        resolver = new CommonResolver();
    }

    @Whitelisted
    public void install(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareNpmStep(args, Arrays.asList(PATH, JAVA_ARGS, ARGS, BUILD_INFO, MODULE));
        stepVariables.put(ARGS, args.get(ARGS));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmInstall", stepVariables);
    }

    @Whitelisted
    public void publish(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareNpmStep(args, Arrays.asList(PATH, JAVA_ARGS, BUILD_INFO, MODULE));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmPublish", stepVariables);
    }

    private Map<String, Object> prepareNpmStep(Map<String, Object> args, List<String> keysAsList) {
        Set<String> npmArgumentsSet = args.keySet();
        if (!keysAsList.containsAll(npmArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getRunArguments((String) args.get(PATH), (BuildInfo) args.get(BUILD_INFO));
        appendBuildInfo(cpsScript, stepVariables);
        stepVariables.put(MODULE, args.get(MODULE));
        // Added to allow java remote debugging
        stepVariables.put(JAVA_ARGS, args.get(JAVA_ARGS));
        return stepVariables;
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList(REPO, SERVER));
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        setDeployer(deployerArguments, Arrays.asList(REPO, SERVER, DEPLOY_ARTIFACTS, INCLUDE_ENV_VARS));
    }

    private Map<String, Object> getRunArguments(String path, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put(NPM_BUILD, this);
        stepVariables.put(PATH, path);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }
}
