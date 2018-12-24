package org.jfrog.hudson.pipeline.common.docker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by romang on 8/9/16.
 */
public class DockerLayers implements Serializable {
    Map<String, DockerLayer> digestToLayer = new HashMap<String, DockerLayer>();
    List<DockerLayer> layers = new ArrayList<DockerLayer>();

    public void addLayer(DockerLayer layer) {
        digestToLayer.put(layer.getDigest(), layer);
        layers.add(layer);
    }

    public DockerLayer getByDigest(String digest) {
        return digestToLayer.get(digest);
    }
}
