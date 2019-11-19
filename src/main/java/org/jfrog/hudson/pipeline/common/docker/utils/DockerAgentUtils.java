package org.jfrog.hudson.pipeline.common.docker.utils;

import com.google.common.collect.ArrayListMultimap;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.pipeline.common.docker.DockerImage;
import org.jfrog.hudson.util.Credentials;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by romang on 8/15/16.
 */
public class DockerAgentUtils implements Serializable {
    // Docker images cache. Every image which is intersepted by the Build-Info Proxy, is added to this cache,
    // so that it can be used to create the build-info in Artifactory.
    private static final List<DockerImage> images = new CopyOnWriteArrayList<>();

    /**
     * Registers an image to be captured by the build-info proxy.
     *
     * @param launcher
     * @param imageTag
     * @param host
     * @param targetRepo
     * @param artifactsProps
     * @param buildInfoId
     * @param envVars
     * @throws IOException
     * @throws InterruptedException
     */
    public synchronized static void registerImagOnAgents(Launcher launcher, final String imageTag,
            final String host, final String targetRepo, final ArrayListMultimap<String, String> artifactsProps,
                final int buildInfoId, final EnvVars envVars) throws IOException, InterruptedException {
        // Master
        final String imageId = getImageIdFromAgent(launcher, imageTag, host, envVars);
        registerImage(imageId, imageTag, targetRepo, artifactsProps, buildInfoId);

        // Agents
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }
            try {
                node.getChannel().call(new MasterToSlaveCallable<Boolean, IOException>() {
                    public Boolean call() throws IOException {
                        registerImage(imageId, imageTag, targetRepo, artifactsProps, buildInfoId);
                        return true;
                    }
                });
            } catch (Exception e) {
                launcher.getListener().getLogger().println("Could not register docker image " + imageTag +
                        " on Jenkins node '" + node.getDisplayName() + "' due to: " + e.getMessage() +
                        " This could be because this node is now offline."
                );
            }
        }
    }

    /**
     * Registers an image to the images cache, so that it can be captured by the build-info proxy.
     *
     * @param imageId
     * @param imageTag
     * @param targetRepo
     * @param buildInfoId
     * @throws IOException
     */
    private static void registerImage(String imageId, String imageTag, String targetRepo,
            ArrayListMultimap<String, String> artifactsProps, int buildInfoId) throws IOException {
        DockerImage image = new DockerImage(imageId, imageTag, targetRepo, buildInfoId, artifactsProps);
        images.add(image);
    }

    /**
     * Gets a list of registered docker images from the images cache, if it has been
     * registered to the cache for a specific build-info ID and if a docker manifest has been captured for it
     * by the build-info proxy.
     * @param buildInfoId
     * @return
     */
    public static List<DockerImage> getImagesByBuildId(int buildInfoId) {
        List<DockerImage> list = new ArrayList<DockerImage>();
        Iterator<DockerImage> it = images.iterator();
        while (it.hasNext()) {
            DockerImage image = it.next();
            if (image.getBuildInfoId() == buildInfoId && image.hasManifest()) {
                list.add(image);
            }
        }
        return list;
    }

    /**
     * Gets a list of registered docker images from the images cache, if it has been
     * registered to the cache for a specific build-info ID and if a docker manifest has been captured for it
     * by the build-info proxy.
     * Additionally, the methods also removes the returned images from the cache.
     * @param buildInfoId
     * @return
     */
    public static List<DockerImage> getAndDiscardImagesByBuildId(int buildInfoId) {
        List<DockerImage> list = new ArrayList<DockerImage>();
        synchronized(images) {
            Iterator<DockerImage> it = images.iterator();
            while (it.hasNext()) {
                DockerImage image = it.next();
                if (image.getBuildInfoId() == buildInfoId) {
                    if (image.hasManifest()) {
                        list.add(image);
                    }
                    it.remove();
                } else // Remove old images from the cache, for which build-info hasn't been published to Artifactory:
                if (image.isExpired()) {
                    it.remove();
                }
            }
        }
        return list;
    }

    /**
     * Retrieves from all the Jenkins agents all the docker images, which have been registered for a specific build-info ID
     * Only images for which manifests have been captured are returned.
     *
     * @param buildInfoId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<DockerImage> getDockerImagesFromAgents(final int buildInfoId, TaskListener listener) throws IOException, InterruptedException {
        List<DockerImage> dockerImages = new ArrayList<DockerImage>();

        // Collect images from the master:
        dockerImages.addAll(getAndDiscardImagesByBuildId(buildInfoId));

        // Collect images from all the agents:
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }
            try {
                List<DockerImage> partialDockerImages = node.getChannel().call(new MasterToSlaveCallable<List<DockerImage>, IOException>() {
                    public List<DockerImage> call() throws IOException {
                        List<DockerImage> dockerImages = new ArrayList<DockerImage>();
                        dockerImages.addAll(getAndDiscardImagesByBuildId(buildInfoId));
                        return dockerImages;
                    }
                });
                dockerImages.addAll(partialDockerImages);
            } catch (Exception e) {
                listener.getLogger().println("Could not collect docker images from Jenkins node '" + node.getDisplayName() + "' due to: " + e.getMessage());
            }
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
     * @param host     
     * @param envVars
     * @return 
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean pushImage(Launcher launcher, final JenkinsBuildInfoLog log, final String imageTag, final Credentials credentials, final String host, final EnvVars envVars)
            throws IOException, InterruptedException {

        return launcher.getChannel().call(new  MasterToSlaveCallable<Boolean, IOException>() {
            public Boolean call() throws IOException {
                String message = "Pushing image: " + imageTag;
                if (StringUtils.isNotEmpty(host)) {
                    message += " using docker daemon host: " + host;
                }

                log.info(message);
                DockerUtils.pushImage(imageTag, credentials.getUsername(), credentials.getPassword(), host, envVars);
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
     * @param envVars
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean pullImage(Launcher launcher, final String imageTag, Credentials credentials, final String host, final EnvVars envVars)
            throws IOException, InterruptedException {

        return launcher.getChannel().call(new MasterToSlaveCallable<Boolean, IOException>() {
            public Boolean call() throws IOException {
                DockerUtils.pullImage(imageTag, credentials.getUsername(), credentials.getPassword(), host, envVars);
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
     * @param envVars
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean updateImageParentOnAgents(final JenkinsBuildInfoLog log, final String imageTag, final String host, final int buildInfoId, final EnvVars envVars) throws IOException, InterruptedException {
        boolean parentUpdated = updateImageParent(log, imageTag, host, buildInfoId, envVars);
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }
            boolean parentNodeUpdated = node.getChannel().call(new MasterToSlaveCallable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    return updateImageParent(log, imageTag, host, buildInfoId, envVars);
                }
            });
            parentUpdated = parentUpdated ? parentUpdated : parentNodeUpdated;
        }
        return parentUpdated;
    }

    /**
     * Adds the docker image parent (the base image) as property on the docker image.
     * This property will be later added to the generated build-info deployed to Artifactory.
     * @param log
     * @param imageTag
     * @param host
     * @param buildInfoId
     * @param envVars
     * @return
     * @throws IOException
     */
    private static boolean updateImageParent(JenkinsBuildInfoLog log, String imageTag, String host, int buildInfoId, EnvVars envVars) throws IOException {
        boolean parentUpdated = false;
        for (DockerImage image : getImagesByBuildId(buildInfoId)) {
            if (image.getImageTag().equals(imageTag)) {
                String parentId = DockerUtils.getParentId(image.getImageId(), host, envVars);
                if (StringUtils.isNotEmpty(parentId)) {
                    Properties properties = new Properties();
                    properties.setProperty("docker.image.parent", DockerUtils.getShaValue(parentId));
                    image.addBuildInfoModuleProps(properties);
                }
                log.info("Docker build-info captured on '" + image.getAgentName() + "' agent.");
                parentUpdated = true;
            }
        }
        return parentUpdated;
    }

    /**
     * Get image ID from imageTag on the current agent.
     *
     * @param launcher
     * @param imageTag
     * @param host
     * @param envVars
     * @return
     */
    public static String getImageIdFromAgent(Launcher launcher, final String imageTag, final String host, final EnvVars envVars) throws IOException, InterruptedException {
        return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
            public String call() throws IOException {
                return DockerUtils.getImageIdFromTag(imageTag, host, envVars);
            }
        });
    }

    /**
     * Get image parent ID from imageID on the current agent.
     *
     * @param launcher
     * @param imageID
     * @param host
     * @param envVars
     * @return
     */
    public static String getParentIdFromAgent(Launcher launcher, final String imageID, final String host, final EnvVars envVars) throws IOException, InterruptedException {
        return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
            public String call() throws IOException {
                return DockerUtils.getParentId(imageID, host, envVars);
            }
        });
    }
}