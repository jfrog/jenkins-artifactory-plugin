package org.jfrog.hudson.pipeline.docker.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import hudson.Launcher;
import hudson.remoting.Callable;
import org.apache.commons.lang.StringUtils;
import org.jfrog.hudson.pipeline.docker.DockerImage;
import org.jfrog.hudson.pipeline.docker.proxy.BuildInfoProxyManager;

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
     * Check whether the proxy server is up and running on agent(could be Master as well).
     *
     * @param launcher
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean isProxyUp(Launcher launcher) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<Boolean, IOException>() {
            public Boolean call() throws IOException {
                return BuildInfoProxyManager.isUp();
            }
        });
    }

    /**
     * Register image to be captured for building build-info.
     *
     * @param launcher
     * @param imageTag
     * @param host
     * @param targetRepo
     * @param buildInfoId
     * @return ImageId
     * @throws IOException
     * @throws InterruptedException
     */
    public synchronized static String registerImage(Launcher launcher, final String imageTag, final String host, final String targetRepo, final int buildInfoId) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            public String call() throws IOException {
                String imageId = DockerUtils.getImageIdFromTag(imageTag, host);
                imageIdToBuildInfoId.put(imageId, buildInfoId);
                imageIdToImageTag.put(imageId, imageTag);
                imageTagToTargetRepo.put(imageTag, targetRepo);
                return imageId;
            }
        });
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
                DockerImage dockerImage = new DockerImage(digest, imageTag, imageTagToTargetRepo.get(imageTag), content);
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
     * @param launcher
     * @param buildInfoId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<DockerImage> getDockerImagesFromAgent(Launcher launcher, final int buildInfoId) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<List<DockerImage>, IOException>() {
            public List<DockerImage> call() throws IOException {
                List<DockerImage> dockerImages = new ArrayList<DockerImage>();
                dockerImages.addAll(buildInfoIdToDockerImage.get(buildInfoId));
                buildInfoIdToDockerImage.removeAll(buildInfoId);
                return dockerImages;
            }
        });
    }

    /**
     * Execute push docker image on agent
     *
     * @param launcher
     * @param build
     * @param imageTag
     * @param credentialsConfig
     * @param host
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean pushImage(Launcher launcher, final String imageTag, final String username, final String password, final String host)
            throws IOException, InterruptedException {

        return launcher.getChannel().call(new Callable<Boolean, IOException>() {
            public Boolean call() throws IOException {
                DockerUtils.pushImage(imageTag, username, password, host);
                return true;
            }
        });
    }

    /**
     * Execute pull docker image on agent
     *
     * @param launcher
     * @param build
     * @param imageTag
     * @param credentialsConfig
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
     * @param launcher
     * @param imageTag
     * @param host
     * @param buildInfoId
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean updateImageParent(Launcher launcher, final String imageTag, final String host, final int buildInfoId) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<Boolean, IOException>() {
            public Boolean call() throws IOException {
                for (DockerImage image : buildInfoIdToDockerImage.get(buildInfoId)) {
                    if (image.getImageTag().equals(imageTag)) {
                        String parentId = DockerUtils.getParentId(image.getImageId(), host);
                        if (StringUtils.isNotEmpty(parentId)) {
                            Properties properties = new Properties();
                            properties.setProperty("docker.image.parent", DockerUtils.getShaValue(parentId));
                            image.addProperties(properties);
                        }

                        return true;
                    }
                }
                return false;
            }
        });
    }
}

