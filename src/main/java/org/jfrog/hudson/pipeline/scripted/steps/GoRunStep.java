package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.ArtifactoryServer;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.executors.GoRunExecutor;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.builds.GoBuild;
import org.jfrog.hudson.pipeline.common.types.resolvers.Resolver;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

@SuppressWarnings("unused")
public class GoRunStep extends AbstractStepImpl {
    static final String  STEP_NAME = "artifactoryGoRun";
    private BuildInfo buildInfo;
    private GoBuild goBuild;
    private String path;
    private String goCmdArgs;
    private String module;
    private String javaArgs; // Added to allow java remote debugging

    @DataBoundConstructor
    public GoRunStep(BuildInfo buildInfo, GoBuild goBuild, String path, String goCmdArgs, String args, String module, String javaArgs) {
        this.buildInfo = buildInfo;
        this.goBuild = goBuild;
        this.path = path;
        this.goCmdArgs = goCmdArgs;
        this.module = module;
        this.javaArgs = javaArgs;
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<BuildInfo> {

        private transient GoRunStep step;

        @Inject
        public Execution(GoRunStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected BuildInfo runStep() throws Exception {
            GoRunExecutor goRunExecutor = new GoRunExecutor(step.buildInfo, launcher, step.goBuild, step.javaArgs, step.goCmdArgs, ws, step.path, step.module, env, listener, build);
            goRunExecutor.execute();
            return goRunExecutor.getBuildInfo();
        }

        @Override
        public ArtifactoryServer getUsageReportServer() {
            Resolver resolver = step.goBuild.getResolver();
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
            super(GoRunStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Run Artifactory Go command";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}