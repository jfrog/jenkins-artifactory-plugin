package org.jfrog.hudson.pipeline.declarative.steps.maven;

import com.google.inject.Inject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

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

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient final MavenDeployerResolver step;

        @Inject
        public Execution(MavenDeployerResolver step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            BuildDataFile buildDataFile = step.buildDataFile;
            DeclarativePipelineUtils.writeBuildDataFile(rootWs, buildNumber, buildDataFile, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }
}
