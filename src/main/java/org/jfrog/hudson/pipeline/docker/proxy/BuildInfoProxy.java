package org.jfrog.hudson.pipeline.docker.proxy;

import hudson.FilePath;
import hudson.model.Node;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.TrustSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by romang on 7/10/16.
 */
public class BuildInfoProxy implements Serializable {

    private static final long serialVersionUID = 1L;
    private static HttpProxyServer server = null;
    private static String agentName;

    public static void start(int proxyPort, String proxyPublicKey, String proxyPrivateKey, String agentName) {
        stop();
        getLogger().info("Starting Build-Info proxy");
        PemFileCertificateSource fileCertificateSource = CertManager.getCertificateSource(proxyPublicKey, proxyPrivateKey);
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(fileCertificateSource)
                .trustSource(TrustSource.defaultTrustSource())
                .build();

        server = DefaultHttpProxyServer.bootstrap()
                .withPort(proxyPort)
                .withAllowLocalOnly(false)
                .withFiltersSource(new BuildInfoHttpFiltersSource())
                .withManInTheMiddle(mitmManager)
                .withConnectTimeout(0)
                .start();
        getLogger().info("Build-Info proxy certificate public key path: " + proxyPublicKey);
        getLogger().info("Build-Info proxy certificate private key path: " + proxyPrivateKey);
        BuildInfoProxy.agentName = agentName;
    }

    public static boolean isUp() {
        return server != null;
    }

    public static void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public static String getAgentName() {
        return agentName;
    }

    public static void stopAll() throws IOException, InterruptedException {
        stop();
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }
            try {
                node.getChannel().call(new MasterToSlaveCallable<Boolean, IOException>() {
                    public Boolean call() throws IOException {
                        BuildInfoProxy.stop();
                        return true;
                    }
                });
            } catch (InvalidClassException e) {
                getLogger().warning("Failed stopping Build-Info proxy on agent: '" + node.getDisplayName() +
                        "'. It could be because the agent uses a different JDK than the master. " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void startAll(final int port)
            throws IOException, InterruptedException {

        File jenkinsHome = new File(Jenkins.getInstance().getRootDir().getPath());
        File publicCert = new File(jenkinsHome, CertManager.DEFAULT_RELATIVE_CERT_PATH);
        File privateCert = new File(jenkinsHome, CertManager.DEFAULT_RELATIVE_KEY_PATH);

        start(port, publicCert.getPath(), privateCert.getPath(), Jenkins.getInstance().getDisplayName());
        List<Node> nodes = Jenkins.getInstance().getNodes();
        for (Node node : nodes) {
            if (node == null || node.getChannel() == null) {
                continue;
            }
            final String agentCertPath = node.getRootPath() + "/" + CertManager.DEFAULT_RELATIVE_CERT_PATH;
            final String agentKeyPath = node.getRootPath() + "/" + CertManager.DEFAULT_RELATIVE_KEY_PATH;
            final String agentName = node.getDisplayName();

            FilePath remoteCertPath = new FilePath(node.getChannel(), agentCertPath);
            FilePath localCertPath = new FilePath(publicCert);
            localCertPath.copyTo(remoteCertPath);

            FilePath remoteKeyPath = new FilePath(node.getChannel(), agentKeyPath);
            FilePath localKeyPath = new FilePath(privateCert);
            localKeyPath.copyTo(remoteKeyPath);

            try {
                node.getChannel().call(new MasterToSlaveCallable<Boolean, IOException>() {
                    public Boolean call() throws IOException {
                        BuildInfoProxy.start(port, agentCertPath, agentKeyPath, agentName);
                        return true;
                    }
                });
            } catch (InvalidClassException e) {
                getLogger().warning("Failed starting Build-Info proxy on agent: '" + node.getDisplayName() +
                        "'. It could be because the agent uses a different JDK than the master. " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static Logger getLogger() {
        return Logger.getLogger(BuildInfoProxy.class.getName());
    }
}
