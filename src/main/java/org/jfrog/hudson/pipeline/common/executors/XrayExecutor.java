package org.jfrog.hudson.pipeline.common.executors;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryXrayClient;
import org.jfrog.hudson.CredentialsConfig;
import org.jfrog.hudson.pipeline.common.Utils;
import org.jfrog.hudson.pipeline.common.types.ArtifactoryServer;
import org.jfrog.hudson.pipeline.common.types.XrayScanConfig;
import org.jfrog.hudson.pipeline.common.types.XrayScanResult;
import org.jfrog.hudson.util.JenkinsBuildInfoLog;

/**
 * @author Alexei Vainshtein
 */
public class XrayExecutor implements Executor {

    private XrayScanConfig xrayScanConfig;
    private TaskListener listener;
    private ArtifactoryServer server;
    private Run build;
    private XrayScanResult xrayScanResult;

    public XrayExecutor(XrayScanConfig xrayScanConfig, TaskListener listener, ArtifactoryServer server, Run build) {
        this.xrayScanConfig = xrayScanConfig;
        this.listener = listener;
        this.server = server;
        this.build = build;
    }

    @Override
    public void execute() throws Exception {
        Log log = new JenkinsBuildInfoLog(listener);
        CredentialsConfig credentialsConfig = server.createCredentialsConfig();
        ArtifactoryXrayClient client = new ArtifactoryXrayClient(server.getUrl(), credentialsConfig.provideUsername(build.getParent()),
                credentialsConfig.providePassword(build.getParent()), log);
        ProxyConfiguration proxyConfiguration = Utils.getProxyConfiguration(Utils.prepareArtifactoryServer(null, server));
        if (proxyConfiguration != null) {
            client.setProxyConfiguration(proxyConfiguration);
        }
        ArtifactoryXrayResponse buildScanResult = client.xrayScanBuild(xrayScanConfig.getBuildName(), xrayScanConfig.getBuildNumber(), "jenkins");
        xrayScanResult = new XrayScanResult(buildScanResult);

        if (xrayScanResult.isFoundVulnerable()) {
            if (xrayScanConfig.getFailBuild()) {
                throw new XrayScanException(xrayScanResult);
            }
            log.error(xrayScanResult.getScanMessage());
        } else {
            log.info(xrayScanResult.getScanMessage());
        }

        if (StringUtils.isNotEmpty(xrayScanResult.getScanUrl())) {
            log.info("Xray scan details are available at: " + xrayScanResult.getScanUrl());
        }
    }

    public XrayScanResult getXrayScanResult() {
        return xrayScanResult;
    }

    public static class XrayScanException extends Exception {

        XrayScanException(XrayScanResult xrayScanResult) {
            super("Violations were found by Xray: " + xrayScanResult, null, true, false);
        }

        @Override
        public String toString() {
            return getLocalizedMessage();
        }
    }
}
