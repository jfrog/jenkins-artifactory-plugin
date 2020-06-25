package org.jfrog.hudson.pipeline.declarative.steps.common;

import com.google.inject.Inject;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.ArtifactorySynchronousNonBlockingStepExecution;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.util.BuildUniqueIdentifierHelper;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

/**
 * Base class for npm/Go deployer and resolver.
 *
 * @author yahavi
 */
public class NpmGoDeployerResolver extends AbstractStepImpl {

    protected BuildDataFile buildDataFile;

    @DataBoundConstructor
    public NpmGoDeployerResolver(String stepName, String stepId, String serverId) {
        buildDataFile = new BuildDataFile(stepName, stepId).put("serverId", serverId);
    }

    @DataBoundSetter
    public void setRepo(String repo) {
        buildDataFile.put("repo", repo);
    }

    public static class Execution extends ArtifactorySynchronousNonBlockingStepExecution<Void> {

        private transient NpmGoDeployerResolver step;

        @Inject
        public Execution(NpmGoDeployerResolver step, StepContext context) throws IOException, InterruptedException {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            String buildNumber = BuildUniqueIdentifierHelper.getBuildNumber(build);
            writeBuildDataFile(ws, buildNumber, step.buildDataFile, new JenkinsBuildInfoLog(listener));
            return null;
        }
    }
}
