package org.jfrog.hudson.pipeline.docker.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import hudson.Launcher;
import hudson.model.Node;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.pipeline.docker.DockerImage;
import org.jfrog.hudson.pipeline.docker.proxy.BuildInfoProxy;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by romang on 8/15/16.
 */
public class DockerAgentUtils implements Serializable {

    /**
     * Using the structure below to create link between the desired docker image to the build.
     */

    //Contains image id to build-info id for registering imageId to build-info
    private static Multimap<String, Integer> imageIdToBuildInfoId = ArrayListMultimap.create();

    //Contains all the prepared docker images for build-info
    private static Multimap<Integer, DockerImage> buildInfoIdToDockerImage = ArrayListMultimap.create();

    //Image id to image tag
    private static Map<String, String> imageIdToImageTag = new HashMap<String, String>();

    //Image tag to target repository
    private static Map<String, String> imageTagToTargetRepo = new HashMap<String, String>();

    /**
     * Register image to be captured for building build-info.
     *
     * @param imageTag
     * @param host
     * @param targetRepo
     * @param buildInfoId
     * @throws IOException
     * @throws InterruptedException
     */
    public synchronized static void registerImageOnAgents(Launcher launcher, final String imageTag, final String host, final String targetRepo, final int buildInfoId) throws IOException, InterruptedException {
        // Master
        final String imageId = getImageIdFromAgent(launcher, imageTag, host);
        registerImage(imageId, imageTag, host, targetRepo, buildInfoId);

        // Agents
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }
            node.getChannel().call(new Callable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    registerImage(imageId, imageTag, host, targetRepo, buildInfoId);
                    return true;
                }
            });
        }
    }

    private static void registerImage(String imageId, String imageTag, String host, String targetRepo, int buildInfoId) {
        if (!DockerUtils.isDockerHostExists(host)) {
            return;
        }
        imageIdToBuildInfoId.put(imageId, buildInfoId);
        imageIdToImageTag.put(imageId, imageTag);
        imageTagToTargetRepo.put(imageTag, targetRepo);
    }

    /**
     * Capture manifest and extract layers data from it to create docker images build-info
     *
     * @param content
     * @param properties
     */
    public synchronized static void captureContent(String content, Properties properties) {
        try {
            String digest = DockerUtils.getConfigDigest(content);
            for (Integer buildInfoId : imageIdToBuildInfoId.get(digest)) {
                String imageTag = imageIdToImageTag.get(digest);
                DockerImage dockerImage = new DockerImage(digest, imageTag, imageTagToTargetRepo.get(imageTag), content, BuildInfoProxy.getAgentName());
                dockerImage.addProperties(properties);
                buildInfoIdToDockerImage.put(buildInfoId, dockerImage);
            }
            imageIdToBuildInfoId.removeAll(digest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve prepared docker images from an agent(could be master as well) related to the given build-info id
     *
     * @param buildInfoId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<DockerImage> getDockerImagesFromAgents(final int buildInfoId) throws IOException, InterruptedException {
        List<DockerImage> dockerImages = new ArrayList<DockerImage>();

        //Master
        dockerImages.addAll(buildInfoIdToDockerImage.get(buildInfoId));
        buildInfoIdToDockerImage.removeAll(buildInfoId);

        //Agents
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }

            List<DockerImage> partialDockerImages = node.getChannel().call(new Callable<List<DockerImage>, IOException>() {
                public List<DockerImage> call() throws IOException {
                    List<DockerImage> dockerImages = new ArrayList<DockerImage>();
                    dockerImages.addAll(buildInfoIdToDockerImage.get(buildInfoId));
                    buildInfoIdToDockerImage.removeAll(buildInfoId);
                    return dockerImages;
                }
            });
            dockerImages.addAll(partialDockerImages);
        }
        return dockerImages;
    }

    /**
     * Execute push docker image on agent
     *
     * @param launcher
     * @param log
     * @param imageTag
     * @param username
     * @param password
     * @param host     @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean pushImage(Launcher launcher, final JenkinsBuildInfoLog log, final String imageTag, final String username, final String password, final String host)
            throws IOException, InterruptedException {

        return launcher.getChannel().call(new Callable<Boolean, IOException>() {
            public Boolean call() throws IOException {
                String message = "Pushing image: " + imageTag;
                if (StringUtils.isNotEmpty(host)) {
                    message += " using docker daemon host: " + host;
                }

                log.info(message);
                DockerUtils.pushImage(imageTag, username, password, host);
                return true;
            }
        });
    }

    /**
     * Execute pull docker image on agent
     *
     * @param launcher
     * @param imageTag
     * @param username
     * @param password
     * @param host
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean pullImage(Launcher launcher, final String imageTag, final String username, final String password, final String host)
            throws IOException, InterruptedException {

        return launcher.getChannel().call(new Callable<Boolean, IOException>() {
            public Boolean call() throws IOException {
                DockerUtils.pullImage(imageTag, username, password, host);
                return true;
            }
        });
    }

    /**
     * Updates property of parent id for the image provided.
     * Returns false if image was not captured true otherwise.
     *
     * @param log
     * @param imageTag
     * @param host
     * @param buildInfoId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean updateImageParentOnAgents(final JenkinsBuildInfoLog log, final String imageTag, final String host, final int buildInfoId) throws IOException, InterruptedException {
        boolean parentUpdated = updateImageParent(log, imageTag, host, buildInfoId);
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }
            boolean parentNodeUpdated = node.getChannel().call(new Callable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    return updateImageParent(log, imageTag, host, buildInfoId);
                }
            });
            parentUpdated = parentUpdated ? parentUpdated : parentNodeUpdated;
        }
        return parentUpdated;
    }

    private static boolean updateImageParent(JenkinsBuildInfoLog log, String imageTag, String host, int buildInfoId) {
        boolean parentUpdated = false;
        for (DockerImage image : buildInfoIdToDockerImage.get(buildInfoId)) {
            if (image.getImageTag().equals(imageTag)) {
                String parentId = DockerUtils.getParentId(image.getImageId(), host);
                if (StringUtils.isNotEmpty(parentId)) {
                    Properties properties = new Properties();
                    properties.setProperty("docker.image.parent", DockerUtils.getShaValue(parentId));
                    image.addProperties(properties);
                }
                log.info("Docker build-info captured on '" + image.getAgentName() + "' agent.");
                parentUpdated = true;
            }
        }
        return parentUpdated;
    }

    /**
     * Get image Id from imageTag using DockerBuildInfoHelper client on agent
     *
     * @param imageTag
     * @return
     */
    private static String getImageIdFromAgent(Launcher launcher, final String imageTag, final String host) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            public String call() throws IOException {
                return DockerUtils.getImageIdFromTag(imageTag, host);
            }
        });
    }
}