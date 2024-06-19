package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.ArrayListMultimap;

public class GetPropertiesResponse {
    private ArrayListMultimap<String, String> propertiesMap;

    public ArrayListMultimap<String, String> getPropertiesMap() { return propertiesMap; };

    public void setPropertiesMap(ArrayListMultimap<String, String> propertiesMap) { this.propertiesMap = propertiesMap; }
}
