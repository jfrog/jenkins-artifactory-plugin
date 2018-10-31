package org.jfrog.hudson.pipeline.declarative.utils;

import com.fasterxml.jackson.databind.JsonNode;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
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
    public static BuildDataFile readBuildDataFile(TaskListener listener, FilePath ws, final String buildNumber, final String stepName, final String stepId) throws IOException, InterruptedException {
        return getTempDirPath(ws).act(new ReadBuildDataFileCallable(listener, buildNumber, stepName, stepId));
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

    public static ArtifactoryServer getArtifactoryServer(TaskListener listener, Run build, FilePath ws, StepContext context, String buildNumber, String serverId) throws IOException, InterruptedException {
        BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, CreateServerStep.STEP_NAME, serverId);
        if (buildDataFile == null) {
            GetArtifactoryServerExecutor getArtifactoryServerExecutor = new GetArtifactoryServerExecutor(build, context, serverId);
            getArtifactoryServerExecutor.execute();
            return getArtifactoryServerExecutor.getArtifactoryServer();
        }
        String url = buildDataFile.get("url").asText();
        JsonNode credentialsIdJson = buildDataFile.get("credentialsId");
        if (credentialsIdJson == null || credentialsIdJson.isNull()) {
            String username = buildDataFile.get("username").asText();
            String password = buildDataFile.get("password").asText();
            return new ArtifactoryServer(url, username, password);
        }
        String credentialsId = credentialsIdJson.asText();
        return new ArtifactoryServer(url, credentialsId);
    }
}
