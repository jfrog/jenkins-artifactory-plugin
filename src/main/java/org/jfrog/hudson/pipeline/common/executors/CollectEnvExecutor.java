package org.jfrog.hudson.pipeline.common.executors;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.jfrog.hudson.pipeline.common.types.buildInfo.Env;

import java.io.File;
import java.io.IOException;

public class CollectEnvExecutor implements Executor {
    private final Run<?, ?> build;
    private final Env env;
    private final EnvVars envVars;
    private transient TaskListener listener;
    private transient FilePath ws;

    public CollectEnvExecutor(Run<?, ?> build, TaskListener listener, FilePath ws, Env env, EnvVars envVars) {
        this.listener = listener;
        this.ws = ws;
        this.env = env;
        this.envVars = envVars;
        this.build = build;
    }

    public void execute() throws IOException, InterruptedException {
        // Collect env vars & system properties.
        Env collectedEnv = ws.act(new CollectEnvCallable(env.collectBuildParameters(build, listener), envVars));
        // Append the collected env.
        env.append(collectedEnv);
    }

    public static class CollectEnvCallable extends MasterToSlaveFileCallable<Env> {
        private final EnvVars envVars;
        private final Env env;

        CollectEnvCallable(Env env, EnvVars envVars) {
            this.env = env;
            this.envVars = envVars;
        }

        public Env invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
            return env.collectVariables(envVars);
        }
    }
}
