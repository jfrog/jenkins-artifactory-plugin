package org.jfrog.hudson.generic.relocate;

import com.google.common.collect.ArrayListMultimap;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.GetPropertiesHelper;

import java.io.IOException;
import java.util.List;

public class PropsHelper {

    private final Log log;

    public PropsHelper(Log log) {
        this.log = log;
    }

    public ArrayListMultimap<String, String> getPropertiesByPathAndKeyNames(String relativePath, ArtifactoryManager artifactoryManager, List<String> properties) throws IOException {
        GetPropertiesHelper helper = new GetPropertiesHelper(artifactoryManager, log);
        /* TODO: do we want any validation on the relativepath?
         */
        return helper.getProperties(relativePath, properties);
    }
}
