package org.jfrog.hudson.jfpipelines;

import hudson.FilePath;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.pipeline.declarative.BuildDataFile;
import org.jfrog.hudson.pipeline.declarative.steps.JfPipelinesStep;
import org.jfrog.hudson.pipeline.declarative.utils.DeclarativePipelineUtils;
import org.jfrog.hudson.util.SerializationUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    /**
     * Get 'workspace' dir for the input project.
     *
     * @param project - The project
     * @return the 'workspace' dir.
     */
    public static FilePath getWorkspace(Job<?, ?> project) {
        FilePath projectJob = new FilePath(project.getRootDir());
        return projectJob.getParent().sibling("workspace").child(project.getName());
    }

    /**
     * Get JFrog Pipelines server from the global configuration or null if not defined.
     *
     * @return configured JFrog Pipelines server.
     */
    public static JFrogPipelinesServer getPipelinesServer() {
        ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl) Jenkins.get().getDescriptor(ArtifactoryBuilder.class);
        if (descriptor == null) {
            return null;
        }
        JFrogPipelinesServer pipelinesServer = descriptor.getJfrogPipelinesServer();
        if (StringUtils.isBlank(pipelinesServer.getIntegrationUrl())) {
            return null;
        }
        return pipelinesServer;
    }

    /**
     * Return true if the JFrog Pipelines server is well configured.
     *
     * @param pipelinesServer - The server to check
     * @return true if the JFrog Pipelines server is well configured.
     */
    public static boolean isConfigured(JFrogPipelinesServer pipelinesServer) {
        return pipelinesServer != null && StringUtils.isNotBlank(pipelinesServer.getIntegrationUrl());
    }

    /**
     * Create job info map for reporting job queue.
     *
     * @param queueItem - The Jenkins queue item
     * @return Job info map.
     */
    public static Map<String, String> createJobInfo(Queue.Item queueItem) {
        return new HashMap<String, String>() {{
            put("queueId", String.valueOf(queueItem.getId()));
        }};
    }

    /**
     * Create job info map for reporting build 'started' and 'completed'.
     *
     * @param build - The build
     * @return Job info map.
     */
    public static Map<String, String> createJobInfo(Run<?, ?> build) {
        Cause.UserIdCause cause = build.getCause(Cause.UserIdCause.class);
        return new HashMap<String, String>() {{
            put("job-name", build.getParent().getName());
            put("job-number", String.valueOf(build.getNumber()));
            put("start-time", String.valueOf(build.getStartTimeInMillis()));
            if (build.getDuration() > 0) {
                put("duration", String.valueOf(build.getDuration()));
            }
            put("build-url", build.getParent().getAbsoluteUrl() + build.getNumber());
            if (cause != null) {
                put("user", cause.getUserId());
            }
        }};
    }
}
