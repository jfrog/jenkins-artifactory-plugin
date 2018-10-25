package org.jfrog.hudson.pipeline.declarative;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.hudson.maven3.Maven3Builder;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.MavenGradleEnvExtractor;
import org.jfrog.hudson.pipeline.types.MavenBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class MavenStep extends AbstractStepImpl {

    private String goal;
    private String pom;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public MavenStep(String pom, String goals) {
        this.goal = goals == null ? "" : goals;
        this.pom = pom == null ? "" : pom;
    }

    @DataBoundSetter
    public void setBuildInfo(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    private String getGoal() {
        return goal;
    }

    private String getPom() {
        return pom;
    }

    private BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient MavenStep step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        @Override
        protected Void run() throws Exception {
            MavenBuild mavenBuild = new MavenBuild();


            mavenBuild(listener, launcher, build, ws, env, mavenBuild, step.getPom(), step.getGoal(), step.getBuildInfo());
            return null;
        }

        private static BuildInfo mavenBuild(TaskListener listener, Launcher launcher, Run build, FilePath ws, EnvVars env, MavenBuild mavenBuild, String pom, String goals, BuildInfo buildInfoParam) throws Exception {
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, buildInfoParam);
            Deployer deployer = getDeployer(mavenBuild);
            deployer.createPublisherBuildInfoDetails(buildInfo);
            String revision = Utils.extractVcsRevision(new FilePath(ws, pom));
            EnvVars extendedEnv = new EnvVars(env);
            extendedEnv.put(ExtractorUtils.GIT_COMMIT, revision);
            MavenGradleEnvExtractor envExtractor = new MavenGradleEnvExtractor(build,
                    buildInfo, deployer, mavenBuild.getResolver(), listener, launcher);
            FilePath tempDir = ExtractorUtils.createAndGetTempDir(launcher, ws);
            envExtractor.buildEnvVars(tempDir, extendedEnv);
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
            return buildInfo;
        }

        /**
         * The Maven3Builder class is looking for the PATH+JDK environment varibale due to legacy code.
         * In The pipeline flow we need to convert the JAVA_HOME to PATH+JDK in order to reuse the code.
         */
        private static void convertJdkPath(Launcher launcher, EnvVars extendedEnv) {
            String seperator = launcher.isUnix() ? "/" : "\\";
            String java_home = extendedEnv.get("JAVA_HOME");
            if (StringUtils.isNotEmpty(java_home)) {
                if (!StringUtils.endsWith(java_home, seperator)) {
                    java_home += seperator;
                }
                extendedEnv.put("PATH+JDK", java_home + "bin");
            }
        }

        private static Deployer getDeployer(MavenBuild mavenBuild) {
            Deployer deployer = mavenBuild.getDeployer();
            if (deployer == null || deployer.isEmpty()) {
                deployer = MavenDeployer.EMPTY_DEPLOYER;
            }
            return deployer;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(MavenStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "rtMaven";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory maven";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
