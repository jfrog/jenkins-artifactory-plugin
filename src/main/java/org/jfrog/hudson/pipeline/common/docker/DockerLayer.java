package org.jfrog.hudson.pipeline.common.docker;

import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.hudson.pipeline.common.docker.utils.DockerUtils;

import java.io.Serializable;

/**
 * Created by romang on 8/9/16.
 */
public class DockerLayer implements Serializable {
    private String repo;
    private String path;
    private String fileName;
    private String sha1;
    private String digest;

    public DockerLayer(AqlSearchResult.SearchEntry entry) {
        this.repo = entry.getRepo();
        this.path = entry.getPath();
        this.fileName = entry.getName();
        this.sha1 = entry.getActualSha1();
        if (!fileName.equals("manifest.json")) {
            this.digest = DockerUtils.fileNameToDigest(fileName);
        } else {
            this.digest = "sha1:" + sha1;
        }
    }

    public String getFullPath() {
        return repo + "/" + path + "/" + fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSha1() {
        return sha1;
    }

    public String getDigest() {
        return digest;
    }
}
