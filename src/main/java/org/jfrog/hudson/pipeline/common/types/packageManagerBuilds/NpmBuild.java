package org.jfrog.hudson.pipeline.common.types.packageManagerBuilds;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.NpmDeployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.NpmResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.hudson.pipeline.common.Utils.BUILD_INFO;
import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

/**
 * Created by Yahav Itzhak on 26 Dec 2018.
 */
public class NpmBuild extends PackageManagerBuild {

    public NpmBuild() {
        deployer = new NpmDeployer();
        resolver = new NpmResolver();
    }

    @Whitelisted
    public void install(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareNpmStep(args, Arrays.asList("path", "args", "buildInfo"));
        stepVariables.put("args", args.get("args"));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmInstall", stepVariables);
    }

    @Whitelisted
    public void publish(Map<String, Object> args) {
        Map<String, Object> stepVariables = prepareNpmStep(args, Arrays.asList("path", "buildInfo"));
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmPublish", stepVariables);
    }

    private Map<String, Object> prepareNpmStep(Map<String, Object> args, List<String> keysAsList) {
        Set<String> npmArgumentsSet = args.keySet();
        if (!keysAsList.containsAll(npmArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getRunArguments((String) args.get("path"), (BuildInfo) args.get("buildInfo"));
        appendBuildInfo(cpsScript, stepVariables);
        return stepVariables;
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList("repo", "server"));
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        setDeployer(deployerArguments, Arrays.asList("repo", "server", "deployArtifacts", "includeEnvVars"));
    }

    private Map<String, Object> getRunArguments(String path, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("npmBuild", this);
        stepVariables.put("path", path);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }
}
