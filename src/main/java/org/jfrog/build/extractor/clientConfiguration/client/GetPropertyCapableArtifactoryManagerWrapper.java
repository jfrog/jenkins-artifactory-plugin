package org.jfrog.build.extractor.clientConfiguration.client;

import com.google.common.collect.ArrayListMultimap;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.GetProperties;

import java.io.IOException;
import java.util.List;

public class GetPropertyCapableArtifactoryManagerWrapper {
    private final ArtifactoryManager wrappedManager;

    public GetPropertyCapableArtifactoryManagerWrapper(ArtifactoryManager wrappedManager) {
        this.wrappedManager = wrappedManager;
    }

    public ArtifactoryManager getWrappedManager() {
        return wrappedManager;
    }

    public ArrayListMultimap<String, String> getProperties(String relativePath, List<String> propertyKeys) throws IOException {
        GetProperties getPropertiesService = new GetProperties(relativePath, propertyKeys, wrappedManager.log);
        return getPropertiesService.execute(wrappedManager.jfrogHttpClient);
    }
}
