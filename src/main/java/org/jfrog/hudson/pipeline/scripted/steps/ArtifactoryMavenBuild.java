package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.executors.MavenExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.MavenBuild;
import org.jfrog.hudson.pipeline.common.types.deployers.Deployer;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class ArtifactoryMavenBuild extends AbstractStepImpl {
    static final String STEP_NAME = "artifactoryMavenBuild";
    private MavenBuild mavenBuild;
    private String goals;
    private String pom;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public ArtifactoryMavenBuild(MavenBuild mavenBuild, String pom, String goals, BuildInfo buildInfo) {
        this.mavenBuild = mavenBuild;
        this.goals = goals == null ? "" : goals;
        this.pom = pom == null ? "" : pom;
        this.buildInfo = buildInfo;
    }

    private MavenBuild getMavenBuild() {
        return mavenBuild;
    }

    private String getGoals() {
        return goals;
    }

    private String getPom() {
        return pom;
    }

    private BuildInfo getBuildInfo() {
        return buildInfo;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {
    protected static final long serialVersionUID = 1L;
        private transient ArtifactoryMavenBuild step;

        @Inject
        public Execution(ArtifactoryMavenBuild step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            MavenBuild mavenBuild = step.getMavenBuild();
            MavenExecutor mavenExecutor = new MavenExecutor(listener, launcher, build, ws, env, mavenBuild, step.getPom(), step.getGoals(), step.getBuildInfo());
            mavenExecutor.execute();
            return mavenExecutor.getBuildInfo();
        }

        @Override
        public ArtifactoryServer getUsageReportServer() {
            Deployer deployer = step.mavenBuild.getDeployer();
            if (deployer != null) {
                return deployer.getArtifactoryServer();
            }
            Resolver resolver = step.mavenBuild.getResolver();
            if (resolver != null) {
                return resolver.getArtifactoryServer();
            }
            return null;
        }

        @Override
        public String getUsageReportFeatureName() {
            return STEP_NAME;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ArtifactoryMavenBuild.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
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
