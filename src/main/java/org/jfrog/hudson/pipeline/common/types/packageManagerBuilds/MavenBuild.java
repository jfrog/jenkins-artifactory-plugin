package org.jfrog.hudson.pipeline.common.types.packageManagerBuilds;

import com.google.common.collect.Maps;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.MavenDeployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.MavenResolver;

import java.util.Arrays;
import java.util.Map;

import static org.jfrog.hudson.pipeline.common.Utils.appendBuildInfo;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class MavenBuild extends PackageManagerBuild {
    private String opts = "";

    public MavenBuild() {
        deployer = new MavenDeployer();
        resolver = new MavenResolver();
    }

    @Whitelisted
    public MavenResolver getResolver() {
        return (MavenResolver) resolver;
    }

    @Whitelisted
    public String getTool() {
        return this.tool;
    }

    @Whitelisted
    public void setTool(String tool) {
        this.tool = tool;
    }

    @Whitelisted
    public String getOpts() {
        return this.opts;
    }

    @Whitelisted
    public void setOpts(String opts) {
        this.opts = opts;
    }

    @Whitelisted
    public void run(Map<String, Object> args) {
        if (!args.containsKey("goals") || !args.containsKey("pom")) {
            throw new IllegalArgumentException("pom and goals are mandatory arguments.");
        }
        deployer.setCpsScript(cpsScript);
        Map<String, Object> stepVariables = getExecutionArguments((String) args.get("pom"), (String) args.get("goals"), (BuildInfo) args.get("buildInfo"));
        appendBuildInfo(cpsScript, stepVariables);

        // Throws CpsCallableInvocation - Must be the last line in this method
        cpsScript.invokeMethod("artifactoryMavenBuild", stepVariables);
    }

    @Whitelisted
    public void resolver(Map<String, Object> resolverArguments) throws Exception {
        setResolver(resolverArguments, Arrays.asList("releaseRepo", "snapshotRepo", "server"));
    }

    @Whitelisted
    public void deployer(Map<String, Object> deployerArguments) throws Exception {
        setDeployer(deployerArguments, Arrays.asList("releaseRepo", "snapshotRepo", "server", "evenIfUnstable", "deployArtifacts", "includeEnvVars"));
    }

    private Map<String, Object> getExecutionArguments(String pom, String goals, BuildInfo buildInfo) {
        Map<String, Object> stepVariables = Maps.newLinkedHashMap();
        stepVariables.put("mavenBuild", this);
        stepVariables.put("pom", pom);
        stepVariables.put("goals", goals);
        stepVariables.put("buildInfo", buildInfo);
        return stepVariables;
    }
}
