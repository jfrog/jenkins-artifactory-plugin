package org.jfrog.hudson.pipeline.steps;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.build.api.Build;
import org.jfrog.hudson.maven3.Maven3Builder;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.executors.MavenGradleEnvExtractor;
import org.jfrog.hudson.pipeline.types.MavenBuild;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.types.deployers.MavenDeployer;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class ArtifactoryMavenBuild extends AbstractStepImpl {

    private MavenBuild mavenBuild;
    private String goal;
    private String pom;
    private String tool;
    private String opts;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public ArtifactoryMavenBuild(MavenBuild mavenBuild, String tool, String pom, String goals, String opts, BuildInfo buildInfo) {
        this.mavenBuild = mavenBuild;
        this.goal = goals == null ? "" : goals;
        this.pom = pom == null ? "" : pom;
        this.tool = tool == null ? "" : tool;
        this.opts = opts == null ? "" : opts;
        this.buildInfo = buildInfo;
    }

    public MavenBuild getMavenBuild() {
        return mavenBuild;
    }

    public String getTool() {
        return tool;
    }

    public String getOpts() {
        return opts;
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

    public void setMavenBuild(MavenBuild mavenBuild) {
        this.mavenBuild = mavenBuild;
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

        @Override
        protected BuildInfo run() throws Exception {
            BuildInfo buildInfo = Utils.prepareBuildinfo(build, step.getBuildInfo());
            Deployer deployer = getDeployer();
            deployer.createPublisherBuildInfoDetails(buildInfo);
            MavenGradleEnvExtractor envExtractor = new MavenGradleEnvExtractor(build,
                    buildInfo, deployer, step.getMavenBuild().getResolver(), listener, launcher);
            envExtractor.buildEnvVars(ws, env);
            String mavenOpts = step.getOpts() + (env.get("MAVEN_OPTS") != null ? env.get("MAVEN_OPTS") : "");
            mavenOpts = mavenOpts.replaceAll("[\t\r\n]+", " ");
            Maven3Builder maven3Builder = new Maven3Builder(step.getTool(), step.getPom(), step.getGoal(), mavenOpts);
            convertJdkPath();
            boolean result = maven3Builder.perform(build, launcher, listener, env, ws);
            if (!result) {
                build.setResult(Result.FAILURE);
                throw new RuntimeException("Maven build failed");
            }
            Build regularBuildInfo = Utils.getGeneratedBuildInfo(build, env, listener, ws, launcher);
            buildInfo.append(regularBuildInfo);
            return buildInfo;
        }

        /**
         * The Maven3Builder class is looking for the PATH+JDK environment varibale due to legacy code.
         * In The pipeline flow we need to convert the JAVA_HOME to PATH+JDK in order to reuse the code.
         */
        private void convertJdkPath() {
            String seperator = launcher.isUnix() ? "/" : "\\";
            String java_home = env.get("JAVA_HOME");
            if (StringUtils.isNotEmpty(java_home)) {
                if (!StringUtils.endsWith(java_home, seperator)) {
                    java_home += seperator;
                }
                env.put("PATH+JDK", java_home + "bin");
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
            return "ArtifactoryMavenBuild";
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
