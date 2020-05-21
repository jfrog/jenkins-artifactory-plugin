package org.jfrog.hudson.pipeline.common;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributingAction;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class PipelinesDataExtractor extends EnvironmentContributor {

    private static final String JF_PIPELINES_ENV = "JF_PIPELINES";
    public static String data;

    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        r.getActions(EnvironmentContributingAction.class).get(0).buildEnvironment(r, envs);
        data = envs.get(JF_PIPELINES_ENV);
    }

    public static String getData() {
        return data;
    }
}
