package org.jfrog.hudson.pipeline.declarative.steps.maven;

import com.google.inject.Inject;
import hudson.FilePath;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Base class for Gradle deployer and resolver.
 */
@SuppressWarnings("unused")
public class MavenDeployerResolver extends AbstractStepImpl {

    BuildDataFile buildDataFile;

    @DataBoundConstructor
    public MavenDeployerResolver(String stepName, String id, String serverId) {
        buildDataFile = new BuildDataFile(stepName, id).put("serverId", serverId);
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient FilePath ws;

        @Inject(optional = true)
        private transient MavenDeployerResolver step;

        @Override
        protected Void run() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            BuildDataFile buildDataFile = step.buildDataFile;
            DeclarativePipelineUtils.writeBuildDataFile(ws, buildNumber, buildDataFile);
            return null;
        }
    }
}
