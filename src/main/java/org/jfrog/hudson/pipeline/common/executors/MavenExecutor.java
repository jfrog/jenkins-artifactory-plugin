package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.hudson.maven3.Maven3Builder;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.deployers.MavenDeployer;
import org.jfrog.hudson.pipeline.common.types.packageManagerBuilds.MavenBuild;
import org.jfrog.hudson.util.ExtractorUtils;

public class MavenExecutor implements Executor {

    private TaskListener listener;
    private Launcher launcher;
    private Run build;
    private FilePath ws;
    private EnvVars env;
    private MavenBuild mavenBuild;
    private String pom;
    private String goals;
    private BuildInfo buildInfo;

    public MavenExecutor(TaskListener listener, Launcher launcher, Run build, FilePath ws, EnvVars env, MavenBuild mavenBuild, String pom, String goals, BuildInfo buildInfo) {
        this.listener = listener;
        this.launcher = launcher;
        this.build = build;
        this.ws = ws;
        this.env = env;
        this.mavenBuild = mavenBuild;
        this.pom = pom;
        this.goals = goals;
        this.buildInfo = Utils.prepareBuildinfo(build, buildInfo);
    }

    public BuildInfo getBuildInfo() {
        return this.buildInfo;
    }

    @Override
    public void execute() throws Exception {
        Deployer deployer = getDeployer(mavenBuild);
        deployer.createPublisherBuildInfoDetails(buildInfo);
        String revision = Utils.extractVcsRevision(new FilePath(ws, pom));
        EnvVars extendedEnv = new EnvVars(env);
        extendedEnv.put(ExtractorUtils.GIT_COMMIT, revision);
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        MavenGradleEnvExtractor envExtractor = new MavenGradleEnvExtractor(build,
                buildInfo, deployer, mavenBuild.getResolver(), listener, launcher, tempDir, extendedEnv);
        envExtractor.execute();
        String stepOpts = mavenBuild.getOpts();
        String mavenOpts = stepOpts + (
                extendedEnv.get("MAVEN_OPTS") != null ? (
                        stepOpts.length() > 0 ? " " : ""
                ) + extendedEnv.get("MAVEN_OPTS") : ""
        );
        mavenOpts = mavenOpts.replaceAll("[\t\r\n]+", " ");
        if (!mavenBuild.getResolver().isEmpty()) {
            extendedEnv.put(BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED, Boolean.TRUE.toString());
        }
        Maven3Builder maven3Builder = new Maven3Builder(mavenBuild.getTool(), pom, goals, mavenOpts);
        convertJdkPath(launcher, extendedEnv);
        boolean buildResult = maven3Builder.perform(build, launcher, listener, extendedEnv, ws, tempDir);
        if (!buildResult) {
            throw new RuntimeException("Maven build failed");
        }
        String generatedBuildPath = extendedEnv.get(BuildInfoFields.GENERATED_BUILD_INFO);
        buildInfo.append(Utils.getGeneratedBuildInfo(build, listener, launcher, generatedBuildPath));
        buildInfo.appendDeployableArtifacts(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS), tempDir, listener);
        buildInfo.setAgentName(Utils.getAgentName(ws));
    }

    /**
     * The Maven3Builder class is looking for the PATH+JDK environment variable due to legacy code.
     * In The pipeline flow we need to convert the JAVA_HOME to PATH+JDK in order to reuse the code.
     */
    private void convertJdkPath(Launcher launcher, EnvVars extendedEnv) {
        String separator = launcher.isUnix() ? "/" : "\\";
        String java_home = extendedEnv.get("JAVA_HOME");
        if (StringUtils.isNotEmpty(java_home)) {
            if (!StringUtils.endsWith(java_home, separator)) {
                java_home += separator;
            }
            extendedEnv.put("PATH+JDK", java_home + "bin");
        }
    }

    private Deployer getDeployer(MavenBuild mavenBuild) {
        Deployer deployer = mavenBuild.getDeployer();
        if (deployer == null || deployer.isEmpty()) {
            deployer = MavenDeployer.EMPTY_DEPLOYER;
        }
        return deployer;
    }
}
