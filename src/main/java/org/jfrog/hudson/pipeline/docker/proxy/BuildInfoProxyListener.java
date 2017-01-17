package org.jfrog.hudson.pipeline.docker.proxy;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.ComputerListener;
import org.jfrog.hudson.pipeline.Utils;
import org.jfrog.hudson.util.plugins.PluginsUtils;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by romang on 8/14/16.
 */
@Extension
public class BuildInfoProxyListener extends ComputerListener implements Serializable {

    @Override
    public void onOnline(final Computer c, TaskListener listener) throws IOException, InterruptedException {
        final boolean proxyEnabled = PluginsUtils.isProxyEnabled();
        final int port = PluginsUtils.getProxyPort();

        if (proxyEnabled && c.getChannel() != null) {
            Utils.copyCertsToAgent(c);
            final String publicKey = c.getNode().getRootPath() + "/" + CertManager.DEFAULT_RELATIVE_CERT_PATH;
            final String privateKey = c.getNode().getRootPath() + "/" + CertManager.DEFAULT_RELATIVE_KEY_PATH;
            final String agentName = c.getNode().getDisplayName();

            c.getChannel().call(new Callable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    BuildInfoProxy.start(port, publicKey, privateKey, agentName);
                    return true;
                }
            });
        }
        super.onOnline(c, listener);
    }
}
