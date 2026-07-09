package org.jfrog.hudson.generic;

import com.google.common.collect.ArrayListMultimap;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.hudson.generic.relocate.PropsHelper;
import org.jfrog.hudson.util.Credentials;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GetPropertiesCallable extends MasterToSlaveFileCallable<ArrayListMultimap<String, String>> {
    private Log log;
    private String username;
    private String password;
    private String accessToken;
    private String serverUrl;
    private ProxyConfiguration proxyConfig;
    private String relativePath;
    private List<String> properties;

    public GetPropertiesCallable(Log log, Credentials credentials, String serverUrl, String relativePath, ProxyConfiguration proxyConfig, List<String> properties) {
        this.log = log;
        this.username = credentials.getUsername();
        this.password = credentials.getPassword();
        this.accessToken = credentials.getAccessToken();
        this.serverUrl = serverUrl;
        this.proxyConfig = proxyConfig;
        this.relativePath = relativePath;
        this.properties = properties;
    }

    public ArrayListMultimap<String, String> invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
        if (StringUtils.isEmpty(relativePath)) {
            return ArrayListMultimap.create();
        }
        PropsHelper propsHelper = new PropsHelper(log);
        try (ArtifactoryManager artifactoryManager = new ArtifactoryManager(serverUrl, username, password, accessToken, log)) {
            if (proxyConfig != null) {
                artifactoryManager.setProxyConfiguration(proxyConfig);
            }
            return propsHelper.getPropertiesByPathAndKeyNames(relativePath, artifactoryManager, properties);
        }
    }
}
