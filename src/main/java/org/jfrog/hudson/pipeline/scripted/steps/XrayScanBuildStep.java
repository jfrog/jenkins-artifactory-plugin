package org.jfrog.hudson.pipeline.scripted.steps;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
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
import org.kohsuke.stapler.DataBoundConstructor;

public class XrayScanBuildStep extends AbstractStepImpl {

    private ArtifactoryServer server;
    private XrayScanConfig xrayScanConfig;

    @DataBoundConstructor
    public XrayScanBuildStep(XrayScanConfig xrayScanConfig, ArtifactoryServer server) {
        this.xrayScanConfig = xrayScanConfig;
        this.server = server;
    }

    public ArtifactoryServer getServer() {
        return server;
    }

    public XrayScanConfig getXrayScanConfig() {
        return xrayScanConfig;
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution<XrayScanResult> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient XrayScanBuildStep step;

        @Override
        protected XrayScanResult run() throws Exception {
            XrayScanConfig xrayScanConfig = step.getXrayScanConfig();

            if (StringUtils.isEmpty(xrayScanConfig.getBuildName())) {
                throw new MissingArgumentException("Xray scan build name is mandatory");
            }

            if (StringUtils.isEmpty(xrayScanConfig.getBuildNumber())) {
                throw new MissingArgumentException("Xray scan build number is mandatory");
            }

            Log log = new JenkinsBuildInfoLog(listener);
            ArtifactoryServer server = step.getServer();
            CredentialsConfig credentialsConfig = server.createCredentialsConfig();
            ArtifactoryXrayClient client = new ArtifactoryXrayClient(server.getUrl(), credentialsConfig.provideUsername(build.getParent()),
                    credentialsConfig.providePassword(build.getParent()), log);
            ProxyConfiguration proxyConfiguration = Utils.getProxyConfiguration(Utils.prepareArtifactoryServer(null, server));
            if (proxyConfiguration != null) {
                client.setProxyConfiguration(proxyConfiguration);
            }

            ArtifactoryXrayResponse buildScanResult = client.xrayScanBuild(xrayScanConfig.getBuildName(), xrayScanConfig.getBuildNumber(), "jenkins");
            XrayScanResult xrayScanResult = new XrayScanResult(buildScanResult);

            if (xrayScanResult.isFoundVulnerable()) {
                if (xrayScanConfig.getFailBuild()) {
                    build.setResult(Result.FAILURE);
                }
                log.error(xrayScanResult.getScanMessage());
            } else {
                log.info(xrayScanResult.getScanMessage());
            }

            if (StringUtils.isNotEmpty(xrayScanResult.getScanUrl())) {
                log.info("Xray scan details are available at: " + xrayScanResult.getScanUrl());
            }
            return xrayScanResult;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(XrayScanBuildStep.Execution.class);
        }

        @Override
        // The step is invoked by ArtifactoryServer by the step name
        public String getFunctionName() {
            return "xrayScanBuild";
        }

        @Override
        public String getDisplayName() {
            return "Xray build scanning";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }
    }
}
