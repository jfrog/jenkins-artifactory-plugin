package org.jfrog.hudson.pipeline.declarative.steps.conan;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.executors.ConanExecutor;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public class ConanClientStep extends AbstractStepImpl {

    static final String STEP_NAME = "rtConanClient";
    private final BuildDataFile buildDataFile;
    private String userHome;

    @DataBoundConstructor
    public ConanClientStep(String id) {
        buildDataFile = new BuildDataFile(STEP_NAME, id);
    }

    @DataBoundSetter
    public void setUserHome(String userHome) {
        this.userHome = userHome;
        buildDataFile.put("userHome", userHome);
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final ConanClientStep step;

        @Inject
        public Execution(ConanClientStep step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            FilePath conanHomeDirectory = Utils.getConanHomeDirectory(step.userHome, env, launcher, ws);
            step.setUserHome(conanHomeDirectory.getRemote());
            ConanExecutor conanExecutor = new ConanExecutor(conanHomeDirectory.getRemote(), ws, launcher, listener, env, build);
            conanExecutor.execClientInit();
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            DeclarativePipelineUtils.writeBuildDataFile(rootWs, buildNumber, step.buildDataFile, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ConanClientStep.Execution.class);
        }

        @Override
        public String getFunctionName() {
            return STEP_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Creates new Conan client";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
