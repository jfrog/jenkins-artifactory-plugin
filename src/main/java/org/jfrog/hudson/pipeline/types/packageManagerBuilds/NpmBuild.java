package org.jfrog.hudson.pipeline.types.packageManagerBuilds;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.NpmDeployer;
import org.jfrog.hudson.pipeline.types.resolvers.NpmResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jfrog.hudson.pipeline.Utils.BUILD_INFO;
import static org.jfrog.hudson.pipeline.Utils.appendBuildInfo;

/**
 * Created by Yahav Itzhak on 26 Dec 2018.
 */
public class NpmBuild extends PackageManagerBuild {
    private String executablePath = "";

    public NpmBuild() {
        deployer = new NpmDeployer();
        resolver = new NpmResolver();
    }

    @Whitelisted
    public String getExecutablePath() {
        return executablePath;
    }

    @Whitelisted
    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    @Whitelisted
    public void install(Map<String, Object> args) {
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmInstall", prepareNpmStep(args));
    }

    @Whitelisted
    public void publish(Map<String, Object> args) {
        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryNpmPublish", prepareNpmStep(args));
    }

    private Map<String, Object> prepareNpmStep(Map<String, Object> args) {
        Set<String> npmArgumentsSet = args.keySet();
        List<String> keysAsList = Arrays.asList("path", "args", "buildInfo");
        if (!keysAsList.containsAll(npmArgumentsSet)) {
            throw new IllegalArgumentException("Only the following arguments are allowed: " + keysAsList.toString());
        }

        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getRunArguments((String) args.get("path"), (String) args.get("args"), (BuildInfo) args.get("buildInfo"));
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

    private Map<String, Object> getRunArguments(String path, String args, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("npmBuild", this);
        stepVariables.put("path", path);
        stepVariables.put("args", args);
        stepVariables.put(BUILD_INFO, buildInfo);
        return stepVariables;
    }


}
