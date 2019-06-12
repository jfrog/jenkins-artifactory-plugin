package org.jfrog.hudson.pipeline.common.executors;

import com.google.common.collect.ArrayListMultimap;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Module;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.docker.DockerImage;
import org.jfrog.hudson.pipeline.common.docker.utils.DockerAgentUtils;
import org.jfrog.hudson.pipeline.common.docker.utils.DockerUtils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfo;
import org.jfrog.hudson.pipeline.common.types.buildInfo.BuildInfoAccessor;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.util.Properties;

/**
 * @author Alexei Vainshtein
 */

public class DockerExecutor implements Executor {

    private ArtifactoryServer pipelineServer;
    private BuildInfo buildInfo;
    private Run build;
    private String imageTag;
    private String targetRepo;
    private String host;
    private Launcher launcher;
    private ArrayListMultimap<String, String> properties;
    private TaskListener listener;

    public DockerExecutor(ArtifactoryServer pipelineServer, BuildInfo buildInfo, Run build, String imageTag, String targetRepo, String host, Launcher launcher, ArrayListMultimap<String, String> properties, TaskListener listener) {
        this.pipelineServer = pipelineServer;
        this.buildInfo = buildInfo;
        this.build = build;
        this.imageTag = imageTag;
        this.targetRepo = targetRepo;
        this.host = host;
        this.launcher = launcher;
        this.properties = properties;
        this.listener = listener;
    }

    @Override
    public void execute() throws Exception {
        JenkinsBuildInfoLog logger = new JenkinsBuildInfoLog(listener);

        String username = pipelineServer.createCredentialsConfig().provideUsername(build.getParent());
        String password = pipelineServer.createCredentialsConfig().providePassword(build.getParent());

        String imageId = DockerAgentUtils.getImageIdFromAgent(launcher, imageTag, host);
        DockerAgentUtils.pushImage(launcher, logger, imageTag, username, password, host);
        DockerImage image = new DockerImage(imageId, imageTag, targetRepo, buildInfo.hashCode(), properties);

        String parentId = DockerAgentUtils.getParentIdFromAgent(launcher, imageId, host);
        if (!StringUtils.isEmpty(parentId)) {
            Properties properties = new Properties();
            properties.put("docker.image.parent", DockerUtils.getShaValue(parentId));
            image.addBuildInfoModuleProps(properties);
        }

        String timestamp = Long.toString(buildInfo.getStartDate().getTime());
        ArtifactoryConfigurator config = new ArtifactoryConfigurator(Utils.prepareArtifactoryServer(null, pipelineServer));
        Module module = image.generateBuildInfoModule(build, listener, config, buildInfo.getName(), buildInfo.getNumber(), timestamp);

        if (module.getArtifacts() == null || module.getArtifacts().size() == 0) {
            logger.warn("Could not find docker image: " + imageTag + " in Artifactory.");
        }

        BuildInfoAccessor buildInfoAccessor = new BuildInfoAccessor(buildInfo);
        buildInfoAccessor.getModules().add(module);

        logger.info("Successfully pushed docker image: " + imageTag);

    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
