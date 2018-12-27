package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import com.google.inject.Inject;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

/**
 * Base class for Gradle deployer and resolver.
 */
@SuppressWarnings("unused")
public class GradleDeployerResolver extends AbstractStepImpl {

    BuildDataFile buildDataFile;

    @DataBoundConstructor
    public GradleDeployerResolver(String stepName, String stepId, String serverId) {
        buildDataFile = new BuildDataFile(stepName, stepId).put("serverId", serverId);
    }

    @DataBoundSetter
    public void setRepo(String repo) {
        buildDataFile.put("repo", repo);
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @Inject(optional = true)
        private transient GradleDeployerResolver step;

        @Override
        protected Void run() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            BuildDataFile buildDataFile = step.buildDataFile;
            writeBuildDataFile(ws, buildNumber, buildDataFile, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }
}
