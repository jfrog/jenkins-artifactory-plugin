package org.jfrog.hudson.pipeline.declarative.steps;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils.writeBuildDataFile;

public class MavenDeployerResolver extends AbstractStepImpl {

    private ObjectNode mavenJson;

    @DataBoundConstructor
    public MavenDeployerResolver(String stepName, String id, String releaseRepo, String snapshotRepo, String serverId) {
        mavenJson = Utils.mapper().createObjectNode();
        mavenJson.put("id", id).
                put("stepName", stepName).
                put("snapshotRepo", snapshotRepo).
                put("releaseRepo", releaseRepo).
                put("serverId", serverId);
    }

    private ObjectNode getMavenJson() {
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
            String buildNumber = DeclarativePipelineUtils.getBuildNumber(getContext());
            ObjectNode mavenJson = step.getMavenJson();
            writeBuildDataFile(ws, buildNumber, mavenJson.get("stepName").asText(), mavenJson.get("id").asText(), mavenJson.toString());
            return null;
        }
    }
}
