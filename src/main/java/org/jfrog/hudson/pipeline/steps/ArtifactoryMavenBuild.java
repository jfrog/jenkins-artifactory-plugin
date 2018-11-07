package org.jfrog.hudson.pipeline.steps;

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
import org.jfrog.hudson.pipeline.executors.EnvExtractor;
import org.jfrog.hudson.pipeline.types.MavenBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.jfrog.hudson.util.ExtractorUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class ArtifactoryMavenBuild extends AbstractStepImpl {

    private MavenBuild mavenBuild;
    private String goal;
    private String pom;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public ArtifactoryMavenBuild(MavenBuild mavenBuild, String pom, String goals, BuildInfo buildInfo) {
        this.mavenBuild = mavenBuild;
        this.goal = goals == null ? "" : goals;
        this.pom = pom == null ? "" : pom;
        this.buildInfo = buildInfo;
    }

    public MavenBuild getMavenBuild() {
        return mavenBuild;
    }

    public String getGoal() {
        return goal;
    }

    public String getPom() {
        return pom;
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<BuildInfo> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient Launcher launcher;

        @Inject(optional = true)
        private transient ArtifactoryMavenBuild step;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient EnvVars env;

        private transient EnvVars extendedEnv;

        @Override
        protected BuildInfo run() throws Exception {
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            Deployer deployer = getDeployer();
            deployer.createPublisherBuildInfoDetails(buildInfo);
            String revision = Utils.extractVcsRevision(new FilePath(ws, step.getPom()));
            extendedEnv = new EnvVars(env);
            extendedEnv.put(ExtractorUtils.GIT_COMMIT, revision);
            EnvExtractor envExtractor = new EnvExtractor(build,
                    buildInfo, deployer, step.getMavenBuild().getResolver(), listener, launcher);
            FilePath tempDir = ExtractorUtils.createAndGetTempDir(launcher, ws);
            envExtractor.buildEnvVars(tempDir, extendedEnv);
            String stepOpts = step.getMavenBuild().getOpts();
            String mavenOpts = stepOpts + (
                    extendedEnv.get("MAVEN_OPTS") != null ? (
                            stepOpts.length() > 0 ? " " : ""
                    ) + extendedEnv.get("MAVEN_OPTS") : ""
            );
            mavenOpts = mavenOpts.replaceAll("[\t\r\n]+", " ");
            if (!step.getMavenBuild().getResolver().isEmpty()) {
                extendedEnv.put(BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED, Boolean.TRUE.toString());
            }
            Maven3Builder maven3Builder = new Maven3Builder(step.getMavenBuild().getTool(), step.getPom(), step.getGoal(), mavenOpts);
            convertJdkPath();
            boolean result = maven3Builder.perform(build, launcher, listener, extendedEnv, ws, tempDir);
            if (!result) {
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
        private void convertJdkPath() {
            String seperator = launcher.isUnix() ? "/" : "\\";
            String java_home = extendedEnv.get("JAVA_HOME");
            if (StringUtils.isNotEmpty(java_home)) {
                if (!StringUtils.endsWith(java_home, seperator)) {
                    java_home += seperator;
                }
                extendedEnv.put("PATH+JDK", java_home + "bin");
            }
        }

        private Deployer getDeployer() {
            Deployer deployer = step.getMavenBuild().getDeployer();
            if (deployer == null || deployer.isEmpty()) {
                deployer = MavenDeployer.EMPTY_DEPLOYER;
            }
            return deployer;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ArtifactoryMavenBuild.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "artifactoryMavenBuild";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory maven";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
