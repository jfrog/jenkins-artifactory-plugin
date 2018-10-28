package org.jfrog.hudson.pipeline.declarative.steps.maven;

import com.google.inject.Inject;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

public class MavenDeployerResolver extends AbstractStepImpl {

    BuildDataFile buildDataFile;

    @DataBoundConstructor
    public MavenDeployerResolver(String stepName, String id, String releaseRepo, String snapshotRepo, String serverId) {
        buildDataFile = new BuildDataFile(stepName, id);
        buildDataFile.put("snapshotRepo", snapshotRepo).
                put("releaseRepo", releaseRepo).
                put("serverId", serverId);
    }

    private BuildDataFile getBuildDataFile() {
        return buildDataFile;
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
            BuildDataFile buildDataFile = step.getBuildDataFile();
            writeBuildDataFile(ws, buildNumber, buildDataFile);
            return null;
        }
    }
}
