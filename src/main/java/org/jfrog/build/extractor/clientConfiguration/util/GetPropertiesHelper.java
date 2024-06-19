package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.ArrayListMultimap;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.GetPropertyCapableArtifactoryManagerWrapper;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;
import java.util.List;

public class GetPropertiesHelper {

    private final GetPropertyCapableArtifactoryManagerWrapper artifactoryManager;
    private final Log log;

    public GetPropertiesHelper(ArtifactoryManager artifactoryManager, Log log) {
        this.artifactoryManager = new GetPropertyCapableArtifactoryManagerWrapper(artifactoryManager);
        this.log = log;
    }

    public ArrayListMultimap<String, String> getProperties(String relativePath, List<String> properties) throws IOException {
        return artifactoryManager.getProperties(relativePath, properties);
    }

    private ArrayListMultimap<String, String> getPropertiesOnResults(List<AqlSearchResult.SearchEntry> searchResults, List<String> properties) throws IOException {
        ArrayListMultimap<String, String> resultMap = ArrayListMultimap.create();
        log.info("Getting properties...");
        String relativePath = buildEntryUrl(searchResults.get(0));
        return artifactoryManager.getProperties(relativePath, properties);
    }

    private String buildEntryUrl(AqlSearchResult.SearchEntry result) {
        String path = result.getPath().equals(".") ? "" : result.getPath() + "/";
        return result.getRepo() + "/" + path + result.getName();
    }
}
