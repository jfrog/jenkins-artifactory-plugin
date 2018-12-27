package org.jfrog.hudson.generic;

import com.google.common.collect.Lists;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by diman on 21/06/2017.
 */
public class FilesResolverCallable extends MasterToSlaveFileCallable<List<Dependency>> {
    private Log log;
    private String username;
    private String password;
    private String serverUrl;
    private String downloadSpec;
    private ProxyConfiguration proxyConfig;

    public FilesResolverCallable(Log log, String username, String password, String serverUrl, String downloadSpec,
                                 ProxyConfiguration proxyConfig) throws IOException, InterruptedException {
        this.log = log;
        this.username = username;
        this.password = password;
        this.serverUrl = serverUrl;
        this.downloadSpec = downloadSpec;
        this.proxyConfig = proxyConfig;
    }

    public List<Dependency> invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
        if (StringUtils.isEmpty(downloadSpec)) {
            return Lists.newArrayList();
        }
        SpecsHelper specsHelper = new SpecsHelper(log);
        ArtifactoryDependenciesClient client = new ArtifactoryDependenciesClient(serverUrl, username, password, log);
        if (proxyConfig != null) {
            client.setProxyConfiguration(proxyConfig);
        }
        try {
            return specsHelper.downloadArtifactsBySpec(downloadSpec, client, file.getCanonicalPath());
        } finally {
            client.close();
        }
    }
}