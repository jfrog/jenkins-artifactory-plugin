package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.hudson.maven3.Maven3Builder;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.MavenBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.deployers.MavenDeployer;
import org.jfrog.hudson.util.ExtractorUtils;

import static org.jfrog.hudson.pipeline.common.types.deployers.MavenDeployer.addDeployedArtifactsActionFromModules;

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
        EnvVars extendedEnv = new EnvVars(env);
        ExtractorUtils.addVcsDetailsToEnv(new FilePath(ws, pom), extendedEnv, listener);
        FilePath tempDir = ExtractorUtils.createAndGetTempDir(ws);
        EnvExtractor envExtractor = new MavenGradleEnvExtractor(build,
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
        maven3Builder.perform(build, launcher, listener, extendedEnv, ws, tempDir);

        String generatedBuildPath = extendedEnv.get(BuildInfoFields.GENERATED_BUILD_INFO);
        org.jfrog.build.api.Build generatedBuild = Utils.getGeneratedBuildInfo(build, listener, launcher, generatedBuildPath);
        // Add action only if artifacts were actually deployed.
        if (deployer.isDeployArtifacts()) {
            addDeployedArtifactsActionFromModules(this.build, deployer.getArtifactoryServer().getArtifactoryUrl(), generatedBuild.getModules());
        }
        buildInfo.append(generatedBuild);

        // Read the deployable artifacts map from the 'json' file in the agent and append them to the buildInfo object.
        buildInfo.getAndAppendDeployableArtifactsByModule(extendedEnv.get(BuildInfoFields.DEPLOYABLE_ARTIFACTS),
                "", tempDir, listener, DeployDetails.PackageType.MAVEN);
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
