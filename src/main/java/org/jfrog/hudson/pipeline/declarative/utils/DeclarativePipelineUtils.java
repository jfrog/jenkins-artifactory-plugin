package org.jfrog.hudson.pipeline.declarative.utils;

import com.fasterxml.jackson.databind.JsonNode;
import hudson.FilePath;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.declarative.steps.CreateServerStep;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.executors.GetArtifactoryServerExecutor;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;

import java.io.IOException;

public class DeclarativePipelineUtils {

    /**
     * Create pipeline build data in @tmp directory.
     * Used to pass data from different steps in declarative pipelines.
     * @param ws - The agent workspace.
     * @param buildNumber - The build number.
     * @param buildDataFile - The build data file to save.
     * @throws Exception - In case of no write permissions.
     */
    public static void writeBuildDataFile(FilePath ws, String buildNumber, BuildDataFile buildDataFile) throws Exception {
        getTempDirPath(ws).act(new CreateBuildDataFileCallable(buildNumber, buildDataFile));
    }

    /**
     * Read pipeline build data from @tmp directory.
     * Used to pass data from different steps in declarative pipelines.
     * @param buildNumber - The build number.
     * @param stepName - The step name - One of 'artifactoryMaven', 'mavenDeploy', 'mavenResolve', 'buildInfo' and other declarative pipeline steps.
     * @param stepId - The step id specified in the pipeline.
     * @throws IOException - In case of no read permissions.
     */
    public static JsonNode readBuildDataFile(FilePath ws, final String buildNumber, final String stepName, final String stepId) throws IOException, InterruptedException {
        return getTempDirPath(ws).act(new ReadBuildDataFileCallable(buildNumber, stepName, stepId));
    }

    private static FilePath getTempDirPath(FilePath ws) {
        return new FilePath(ws.getParent(), ws.getName() + "@tmp");
    }

    static String getBuildDataFileName(String stepName, String stepId) {
        return stepName + "_" + stepId;
    }

    public static String getBuildNumberFromStep(StepContext getContext) throws IOException, InterruptedException {
        WorkflowRun workflowRun = getContext.get(WorkflowRun.class);
        if (workflowRun == null) {
            throw new IOException("Step has no workflow");
        }
        return workflowRun.getId();
    }

    public static ArtifactoryServer getArtifactoryServer(Run build, FilePath ws, StepContext context, String buildNumber, JsonNode jsonNode) throws IOException, InterruptedException {
        JsonNode serverId = jsonNode.get("serverId");
        if (serverId.isNull()) {
            return null;
        }
        jsonNode = DeclarativePipelineUtils.readBuildDataFile(ws, buildNumber, CreateServerStep.STEP_NAME, serverId.asText());
        if (jsonNode.isNull()) {
            GetArtifactoryServerExecutor getArtifactoryServerExecutor = new GetArtifactoryServerExecutor(build, context, serverId.asText());
            getArtifactoryServerExecutor.execute();
            return getArtifactoryServerExecutor.getArtifactoryServer();
        }
        String url = jsonNode.get("url").asText();
        JsonNode credentialsIdJson = jsonNode.get("credentialsId");
        if (credentialsIdJson == null || credentialsIdJson.isNull()) {
            String username = jsonNode.get("username").asText();
            String password = jsonNode.get("password").asText();
            return new ArtifactoryServer(url, username, password);
        }
        String credentialsId = credentialsIdJson.asText();
        return new ArtifactoryServer(url, credentialsId);
    }

    public static boolean isJsonNodeNotNull(JsonNode jsonNode) {
        return jsonNode != null && !jsonNode.isNull();
    }
}
