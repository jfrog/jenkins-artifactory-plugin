package org.jfrog.hudson.pipeline.common.types.buildInfo;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.jfrog.build.api.Module;
import org.jfrog.hudson.pipeline.common.ArtifactoryConfigurator;
import org.jfrog.hudson.pipeline.common.docker.DockerImage;
import org.jfrog.hudson.pipeline.common.docker.utils.DockerAgentUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by romang on 7/28/16.
 */
public class DockerBuildInfoHelper implements Serializable {

    private BuildInfo buildInfo;
    private List<Integer> aggregatedBuildInfoIds = new ArrayList<Integer>();

    public DockerBuildInfoHelper(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    public List<Module> generateBuildInfoModules(Run build, TaskListener listener, ArtifactoryConfigurator config) throws IOException, InterruptedException {
        aggregatedBuildInfoIds.add(buildInfo.hashCode());
        List<DockerImage> dockerImages = new ArrayList<DockerImage>();
        for (Integer buildInfoId : aggregatedBuildInfoIds) {
            dockerImages.addAll(DockerAgentUtils.getDockerImagesFromAgents(buildInfoId, listener));
        }

        String timestamp = Long.toString(buildInfo.getStartDate().getTime());
        ArrayList<Module> modules = new ArrayList<Module>();
        for (DockerImage dockerImage : dockerImages) {
            modules.add(dockerImage.generateBuildInfoModule(build, listener, config, buildInfo.getName(), buildInfo.getNumber(), timestamp));
        }
        return modules;
    }

    public void append(DockerBuildInfoHelper other) {
        aggregatedBuildInfoIds.add(other.buildInfo.hashCode());
    }
}
