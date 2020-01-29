package org.jfrog.hudson.pipeline.common.docker.utils;

public class DockerLayersDescriptions {
    String digest;
    String mediaType;

    public DockerLayersDescriptions(String digest, String type) {
        this.digest = digest;
        this.mediaType = type;
    }

    public String getDigest() {
        return digest;
    }

    public String getMediaType() {
        return mediaType;
    }

    public boolean isForeignLayer(){
       return mediaType.equals("application/vnd.docker.image.rootfs.foreign.diff.tar.gzip");
    }
}