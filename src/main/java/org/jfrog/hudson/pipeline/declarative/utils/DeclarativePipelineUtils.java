package org.jfrog.hudson.pipeline.declarative.utils;

import com.fasterxml.jackson.databind.JsonNode;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.IOException;

public class DeclarativePipelineUtils {

    /**
     * Create pipeline build data in @tmp directory.
     * The build data can be one of 'artifactoryMaven', 'mavenDeploy', 'mavenResolve', 'buildInfo' and other declarative pipeline steps.
     * @param buildNumber - The build number
     * @param stepName - The step name - On of 'artifactoryMaven', 'mavenDeploy', 'mavenResolve', 'buildInfo' and other declarative pipeline steps.
     * @param stepId - The step id from the user.
     * @param content - The content of the data file.
     * @throws Exception - In case of missing permissions.
     */
    public static void writeBuildDataFile(FilePath ws, String buildNumber, String stepName, String stepId, String content) throws Exception {
        getTempDirPath(ws).act(new CreateBuildFileCallable(buildNumber, stepName, stepId, content));
    }

    /**
     * Read pipeline build data from @tmp directory.
     * The build data can be one of 'artifactoryMaven', 'mavenDeploy', 'mavenResolve', 'buildInfo' and other declarative pipeline steps.
     * @param buildNumber - The build number
     * @param stepName - The step name - On of 'artifactoryMaven', 'mavenDeploy', 'mavenResolve', 'buildInfo' and other declarative pipeline steps.
     * @param stepId - The step id from the user.
     */
    public static JsonNode readBuildDataFile(FilePath ws, final String buildNumber, final String stepName, final String stepId) throws IOException, InterruptedException {
        return getTempDirPath(ws).act(new ReadBuildFileCallable(buildNumber, stepName, stepId));
    }

    private static FilePath getTempDirPath(FilePath ws) {
        return new FilePath(ws.getParent(), ws.getName() + "@tmp");
    }

    static String getBuildDataFileName(String stepName, String stepId) {
        return stepName + "_" + stepId;
    }

    public static String getBuildNumber(StepContext getContext) throws IOException, InterruptedException {
        WorkflowRun workflowRun = getContext.get(WorkflowRun.class);
        if (workflowRun == null) {
            throw new IOException("Step has no workflow");
        }
        return workflowRun.getId();
    }
}
