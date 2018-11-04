package org.jfrog.hudson.pipeline.declarative.utils;

import com.fasterxml.jackson.databind.JsonNode;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.pipeline.declarative.steps.BuildInfoStep;
import org.jfrog.hudson.pipeline.declarative.steps.CreateServerStep;
import org.jfrog.hudson.pipeline.declarative.types.BuildDataFile;
import org.jfrog.hudson.pipeline.executors.GetArtifactoryServerExecutor;
import org.jfrog.hudson.pipeline.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.types.buildInfo.BuildInfo;

import java.io.IOException;

public class DeclarativePipelineUtils {

    /**
     * Create pipeline build data in @tmp/build-number directory.
     * Used to transfer data from different steps in declarative pipelines.
     * @param ws - The agent workspace.
     * @param buildNumber - The build number.
     * @param buildDataFile - The build data file to save.
     * @throws Exception - In case of no write permissions.
     */
    public static void writeBuildDataFile(FilePath ws, String buildNumber, BuildDataFile buildDataFile) throws Exception {
        getTempDirPath(ws).act(new CreateBuildDataFileCallable(buildNumber, buildDataFile));
    }

    /**
     * Read pipeline build data from @tmp/build-number directory.
     * Used to transfer data from different steps in declarative pipelines.
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

    /**
     * Get the job build name.
     * @param context - The job context.
     * @return Job's build name.
     */
    public static String getBuildName(StepContext context) throws IOException, InterruptedException {
        WorkflowRun workflowRun = context.get(WorkflowRun.class);
        if (workflowRun == null) {
            throw new IOException("Step has no workflow");
        }
        return StringUtils.substringBefore(workflowRun.getExternalizableId(), "#");
    }

    /**
     * Get the job build number.
     * @param context - The job context.
     * @return Job's build number.
     */
    public static String getBuildNumber(StepContext context) throws IOException, InterruptedException {
        WorkflowRun workflowRun = context.get(WorkflowRun.class);
        if (workflowRun == null) {
            throw new IOException("Step has no workflow");
        }
        return workflowRun.getId();
    }

    /**
     * Get Artifactory server from global server configuration or from previous rtServer{...} scope.
     * @param listener - Step's listener.
     * @param build - Step's build.
     * @param ws - Step's workspace.
     * @param context - Step's context.
     * @param serverId - The server id. Can be defined from global server configuration or from previous rtServer{...} scope.
     * @return Artifactory server.
     */
    public static ArtifactoryServer getArtifactoryServer(TaskListener listener, Run build, FilePath ws, StepContext context, String serverId) throws IOException, InterruptedException {
        String buildNumber = getBuildNumber(context);
        BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, buildNumber, CreateServerStep.STEP_NAME, serverId);
        if (buildDataFile == null) {
            GetArtifactoryServerExecutor getArtifactoryServerExecutor = new GetArtifactoryServerExecutor(build, context, serverId);
            getArtifactoryServerExecutor.execute();
            return getArtifactoryServerExecutor.getArtifactoryServer();
        }
        JsonNode jsonNode = buildDataFile.get(CreateServerStep.STEP_NAME);
        ArtifactoryServer server = Utils.mapper().treeToValue(jsonNode, ArtifactoryServer.class);
        String credentialsId = jsonNode.get("credentialsId").asText();
        if (!credentialsId.isEmpty()) {
            server.setCredentialsId(credentialsId);
        } else {
            server.setUsername(jsonNode.get("username").asText());
            server.setPassword(jsonNode.get("password").asText());
        }
        return server;
    }

    /**
     * Create build info id: <buildname>_<buildnumber>.
     * @param context - Step's context.
     * @param customBuildName - Step's custom build name if exist.
     * @param customBuildNumber - Step's custom build number if exist.
     * @return build info id: <buildname>_<buildnumber>.
     */
    public static String createBuildInfoId(StepContext context, String customBuildName, String customBuildNumber) throws IOException, InterruptedException {
        return (StringUtils.isBlank(customBuildName) ? getBuildName(context) : customBuildName) + "_" +
                (StringUtils.isBlank(customBuildNumber) ? getBuildNumber(context) : customBuildNumber);
    }

    /**
     * Get build info as defined in previous rtBuildInfo{...} scope.
     * @param listener - Step's listener.
     * @param ws - Step's workspace.
     * @param context - Step's context.
     * @param customBuildName - Step's custom build name if exist.
     * @param customBuildNumber - Step's custom build number if exist.
     * @return build info object as defined in previous rtBuildInfo{...} scope or null.
     */
    public static BuildInfo getBuildInfo(TaskListener listener, FilePath ws, StepContext context, String customBuildName, String customBuildNumber) throws IOException, InterruptedException {
        String jobBuildNumber = getBuildNumber(context);
        String buildInfoId = createBuildInfoId(context, customBuildName, customBuildNumber);

        BuildDataFile buildDataFile = DeclarativePipelineUtils.readBuildDataFile(listener, ws, jobBuildNumber, BuildInfoStep.STEP_NAME, buildInfoId);
        if (buildDataFile == null) {
            return null; // Build info doesn't exist. Will create a new build info later.
        }
        return Utils.mapper().treeToValue(buildDataFile.get(BuildInfoStep.STEP_NAME), BuildInfo.class);
    }

    /**
     * Save build info in @tmp/build-number folder.
     * @param buildInfo - The build info object to save.
     * @param ws - Step's workspace.
     * @param context - Step's context.
     */
    public static void saveBuildInfo(BuildInfo buildInfo, FilePath ws, StepContext context) throws Exception {
        String jobBuildNumber = getBuildNumber(context);
        String buildInfoId = createBuildInfoId(context, buildInfo.getName(), buildInfo.getNumber());

        BuildDataFile buildDataFile = new BuildDataFile(BuildInfoStep.STEP_NAME, buildInfoId);
        buildDataFile.putPOJO(buildInfo);
        writeBuildDataFile(ws, jobBuildNumber, buildDataFile);
    }
}
