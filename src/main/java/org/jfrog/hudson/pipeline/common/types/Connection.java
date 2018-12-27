package org.jfrog.hudson.pipeline.common.types;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jfrog.hudson.action.ActionableHelper;

import java.io.Serializable;

/**
 * @author Alexei Vainshtein
 */
public class Connection implements Serializable {
    public static final long serialVersionUID = 1L;

    private int connectionRetry = ActionableHelper.getDefaultConnectionRetries();
    private int timeout = 300;

    @Whitelisted
    public int getTimeout() {
        return timeout;
    }

    @Whitelisted
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Whitelisted
    public int getRetry() {
        return connectionRetry;
    }

    @Whitelisted
    public void setRetry(int connectionRetry) {
        this.connectionRetry = connectionRetry;
    }
}
