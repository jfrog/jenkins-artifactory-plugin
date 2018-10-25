package org.jfrog.hudson.pipeline.declarative;

import com.google.inject.Inject;
import hudson.FilePath;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.jfrog.hudson.pipeline.declarative.utils.Utils.createTempBuildDataFile;

public class MavenDeployerResolver extends AbstractStepImpl {

    private JSONObject mavenJson;

    @DataBoundConstructor
    public MavenDeployerResolver(String stepName, String id, String releaseRepo, String snapshotRepo, String serverId) {
        mavenJson = new JSONObject();
        mavenJson.element("id", id);
        mavenJson.element("stepName", stepName);
        mavenJson.element("releaseRepo", releaseRepo);
        mavenJson.element("snapshotRepo", snapshotRepo);
        mavenJson.element("serverId", serverId);
    }

    private JSONObject getMavenJson() {
        return this.mavenJson;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient FilePath ws;

        @Inject(optional = true)
        private transient MavenDeployerResolver step;

        @Override
        protected Void run() throws Exception {
            String buildNumber = getContext().get(WorkflowRun.class).getId();
            JSONObject mavenJson = step.getMavenJson();
            createTempBuildDataFile(ws, buildNumber, mavenJson.getString("stepName"), mavenJson.getString("id"), mavenJson.toString());
            return null;
        }
    }
}
