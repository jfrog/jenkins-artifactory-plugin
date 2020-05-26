package org.jfrog.hudson.util;

import jenkins.model.Jenkins;
import org.jfrog.build.client.ProxyConfiguration;

public class ProxyUtils {

    public static ProxyConfiguration createProxyConfiguration() {
        hudson.ProxyConfiguration proxy = Jenkins.get().proxy;
        if (proxy == null) {
            return null;
        }
        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.host = proxy.name;
        proxyConfiguration.port = proxy.port;
        proxyConfiguration.username = proxy.getUserName();
        proxyConfiguration.password = proxy.getPassword();
        return proxyConfiguration;
    }

}
