package org.jfrog.hudson.pipeline.common.types.builds;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.GradleDeployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.GradleResolver;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class GradleBuild extends PackageManagerBuild {
    private boolean useWrapper;
    private boolean usesPlugin;

    public GradleBuild() {
        deployer = new GradleDeployer();
        resolver = new GradleResolver();
    }

    @Whitelisted
    public boolean isUsesPlugin() {
        return this.usesPlugin;
    }

    @Whitelisted
    public void setUsesPlugin(boolean usesPlugin) {
        this.usesPlugin = usesPlugin;
    }

    @Whitelisted
    public boolean isUseWrapper() {
        return useWrapper;
    }

    @Whitelisted
    public void setUseWrapper(boolean useWrapper) {
        this.useWrapper = useWrapper;
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList("repo", "server"));
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        setDeployer(deployerArguments, Arrays.asList("repo", "snapshotRepo", "releaseRepo", "server", "deployArtifacts", "includeEnvVars", "usesPlugin", "deployMaven", "deployIvy", "ivyPattern", "artifactPattern", "publications"));
    }

    @Whitelisted
    public void run(Map<String, Object> args) {
        if (!args.containsKey("tasks")) {
            throw new IllegalArgumentException("tasks is a mandatory argument.");
        }
        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getRunArguments((String) args.get("buildFile"), (String) args.get("tasks"), (String) args.get("switches"), (String) args.get("rootDir"), (BuildInfo) args.get("buildInfo"));
        Utils.appendBuildInfo(cpsScript, stepVariables);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("ArtifactoryGradleBuild", stepVariables);
    }

    private Map<String, Object> getRunArguments(String buildFile, String tasks, String switches, String rootDir, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("gradleBuild", this);
        stepVariables.put("rootDir", rootDir);
        stepVariables.put("buildFile", buildFile);
        stepVariables.put("tasks", tasks);
        stepVariables.put("switches", switches);
        stepVariables.put(Utils.BUILD_INFO, buildInfo);
        return stepVariables;
    }
}
