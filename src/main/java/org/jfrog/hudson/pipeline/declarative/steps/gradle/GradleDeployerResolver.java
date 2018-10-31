package org.jfrog.hudson.pipeline.declarative.steps.gradle;

import com.google.inject.Inject;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

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

    private BuildDataFile getBuildDataFile() {
        return buildDataFile;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient FilePath ws;

        @Inject(optional = true)
        private transient GradleDeployerResolver step;

        @Override
        protected Void run() throws Exception {
            String buildNumber = DeclarativePipelineUtils.getBuildNumberFromStep(getContext());
            BuildDataFile buildDataFile = step.getBuildDataFile();
            writeBuildDataFile(ws, buildNumber, buildDataFile);
            return null;
        }
    }
}
