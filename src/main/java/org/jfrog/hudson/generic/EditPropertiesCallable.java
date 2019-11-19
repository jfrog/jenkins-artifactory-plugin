package org.jfrog.hudson.generic;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.hudson.util.Credentials;

import java.io.File;
import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper.EditPropertiesActionType;

public class EditPropertiesCallable extends MasterToSlaveFileCallable<Boolean> {
    private Log log;
    private String username;
    private String password;
    private String accessToken;
    private String serverUrl;
    private String spec;
    private ProxyConfiguration proxyConfig;
    private EditPropertiesActionType editType;
    private String props;

    public EditPropertiesCallable(Log log, Credentials credentials, String serverUrl, String spec,
                                  ProxyConfiguration proxyConfig, EditPropertiesActionType editType, String props)
            throws IOException, InterruptedException {
        this.log = log;
        this.username = credentials.getUsername();
        this.password = credentials.getPassword();
        this.accessToken = credentials.getAccessToken();
        this.serverUrl = serverUrl;
        this.spec = spec;
        this.proxyConfig = proxyConfig;
        this.editType = editType;
        this.props = props;
    }

    public Boolean invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
        if (StringUtils.isEmpty(spec)) {
            return false;
        }
        SpecsHelper specsHelper = new SpecsHelper(log);
        ArtifactoryDependenciesClient client = new ArtifactoryDependenciesClient(serverUrl, username, password, accessToken, log);
        if (proxyConfig != null) {
            client.setProxyConfiguration(proxyConfig);
        }
        try {
            return specsHelper.editPropertiesBySpec(spec, client, editType, props);
        } finally {
            client.close();
        }
    }
}
