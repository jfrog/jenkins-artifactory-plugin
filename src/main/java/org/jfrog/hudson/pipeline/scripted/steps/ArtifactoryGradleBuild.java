package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.*;
import org.jfrog.hudson.pipeline.common.executors.GradleExecutor;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GradleBuild;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by Tamirh on 04/08/2016.
 */
public class ArtifactoryGradleBuild extends AbstractStepImpl {

    private GradleBuild gradleBuild;
    private String tasks;
    private String buildFile;
    private String rootDir;
    private String switches;
    private BuildInfo buildInfo;

    @DataBoundConstructor
    public ArtifactoryGradleBuild(GradleBuild gradleBuild, String rootDir, String buildFile, String tasks, String switches, BuildInfo buildInfo) {
        this.gradleBuild = gradleBuild;
        this.tasks = tasks;
        this.rootDir = rootDir;
        this.buildFile = buildFile;
        this.switches = switches;
        this.buildInfo = buildInfo;
    }

    private GradleBuild getGradleBuild() {
        return gradleBuild;
    }

    private String getSwitches() {
        return switches;
    }

    private String getTasks() {
        return tasks;
    }

    private String getBuildFile() {
        return buildFile;
    }

    private BuildInfo getBuildInfo() {
        return buildInfo;
    }

    private String getRootDir() {
        return rootDir;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient ArtifactoryGradleBuild step;

        @Inject
        public Execution(ArtifactoryGradleBuild step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            GradleExecutor gradleExecutor = new GradleExecutor(build, step.getGradleBuild(), step.getTasks(), step.getBuildFile(), step.getRootDir(), step.getSwitches(), step.getBuildInfo(), env, ws, listener, launcher);
            gradleExecutor.execute();
            return gradleExecutor.getBuildInfo();
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ArtifactoryGradleBuild.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "ArtifactoryGradleBuild";
        }

        @Override
        public String getDisplayName() {
            return "run Artifactory gradle";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
