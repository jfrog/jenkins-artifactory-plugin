package org.jfrog.hudson.pipeline.declarative.steps;

import com.google.inject.Inject;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.types.BuildFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

public class MavenDeployerResolver extends AbstractStepImpl {

    private BuildFile buildFile;

    @DataBoundConstructor
    public MavenDeployerResolver(String stepName, String id, String releaseRepo, String snapshotRepo, String serverId) {
        buildFile = new BuildFile(stepName, id);
        buildFile.put("snapshotRepo", snapshotRepo).
                put("releaseRepo", releaseRepo).
                put("serverId", serverId);
    }

    private BuildFile getBuildFile() {
        return buildFile;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient FilePath ws;

        @Inject(optional = true)
        private transient MavenDeployerResolver step;

        @Override
        protected Void run() throws Exception {
            String buildNumber = DeclarativePipelineUtils.getBuildNumberFromStep(getContext());
            BuildFile buildFile = step.getBuildFile();
            writeBuildDataFile(ws, buildNumber, buildFile);
            return null;
        }
    }
}
